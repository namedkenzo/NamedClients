using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Threading;
using Microsoft.Win32;

namespace NamedP;

public partial class MainWindow : Window
{
    Process? engine, vpn;
    CancellationTokenSource? session;
    bool fullVpn = true, connecting, connected, stopping, closing, closePending;
    DateTime connectedAt;
    Process? testEngine;
    CancellationTokenSource? testSession;
    bool testing;
    readonly DispatcherTimer ticker = new() { Interval = TimeSpan.FromSeconds(1) };
    readonly DispatcherTimer profileSaver = new() { Interval = TimeSpan.FromMilliseconds(600) };
    readonly ProfileVault vault;
    bool profileDirty;
    readonly string settings = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "namedP", "theme.txt");
    public MainWindow() : this(new ProfileVault(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "namedP", "profile.dat"))) { }
    public MainWindow(ProfileVault profileVault)
    {
        vault=profileVault;
        InitializeComponent();
        var version = System.Reflection.CustomAttributeExtensions.GetCustomAttribute<System.Reflection.AssemblyInformationalVersionAttribute>(typeof(MainWindow).Assembly)?.InformationalVersion.Split('+')[0] ?? "development";
        ClientVersion.Text = "namedP · " + version;
        try { if (File.Exists(settings) && int.TryParse(File.ReadAllText(settings), out int index)) Theme.SelectedIndex = Math.Clamp(index, 0, 2); } catch (IOException) { }
        try { Gaming.IsChecked = File.Exists(Path.ChangeExtension(settings,"gaming")) && File.ReadAllText(Path.ChangeExtension(settings,"gaming")).Trim()=="true"; } catch (IOException) { }
        try { Link.Password = vault.Load(); ProfileNote.Text = Link.Password.Length > 0 ? "Restored · protected by your Windows account" : "Your link is saved securely on this Windows account."; }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException or System.Security.Cryptography.CryptographicException)
        { ProfileNote.Text = "Saved link could not be unlocked. Paste a backup link or choose Forget."; }
        profileSaver.Tick += (_,_) => SaveProfile();
        Link.PasswordChanged += (_,_) => {
            profileDirty=true; profileSaver.Stop();profileSaver.Start();
            ProfileNote.Text="Saving securely…";
            if(!connected&&!connecting) { Delay.Text="Not tested"; UpdateModes(); }
        };
        ApplyTheme(); UpdateModes();
        SystemEvents.UserPreferenceChanged += SystemThemeChanged;
        ticker.Tick += (_, _) => { if (connected) Elapsed.Text = (DateTime.UtcNow - connectedAt).ToString(@"hh\:mm\:ss"); };
        ticker.Start(); Closing += OnClosing;
    }
    void SystemThemeChanged(object sender, UserPreferenceChangedEventArgs e) => Dispatcher.BeginInvoke(new Action(ApplyTheme));
    void ThemeChanged(object sender, SelectionChangedEventArgs e)
    {
        if (!IsInitialized) return;
        ApplyTheme();
        try { Directory.CreateDirectory(Path.GetDirectoryName(settings)!); File.WriteAllText(settings, Theme.SelectedIndex.ToString()); } catch (IOException) { }
    }
    void ApplyTheme()
    {
        bool dark = Theme.SelectedIndex == 1;
        if (Theme.SelectedIndex == 0)
        {
            using var key = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Themes\Personalize");
            dark = key?.GetValue("AppsUseLightTheme") is int value && value == 0;
        }
        var colors = dark
            ? new[] { "#0D1917", "#152420", "#EDF6F1", "#9AB4A8", "#2A4037", "#73DFBE", "#1B352B", "#102A20", "#F3B7A5" }
            : new[] { "#F4F6F1", "#FFFFFF", "#162F26", "#5C7369", "#D8E5DD", "#166C56", "#E7F0EA", "#FFFFFF", "#A33B2A" };
        string[] names = ["Bg", "Surface", "Text", "Muted", "Line", "Accent", "Soft", "AccentInk", "Danger"];
        for (int i = 0; i < names.Length; i++) Application.Current.Resources[names[i]] = new SolidColorBrush((Color)ColorConverter.ConvertFromString(colors[i]));
        Application.Current.Resources["HeroBrush"] = new LinearGradientBrush((Color)ColorConverter.ConvertFromString(dark?"#203B30":"#E4F0E7"),(Color)ColorConverter.ConvertFromString(dark?"#152420":"#F4F6F1"),60);
        Dog?.InvalidateVisual();
        if (ProfileMode != null) UpdateModes();
    }
    void GamingChanged(object sender, RoutedEventArgs e)
    {
        if(!IsInitialized)return;
        try{Directory.CreateDirectory(Path.GetDirectoryName(settings)!);File.WriteAllText(Path.ChangeExtension(settings,"gaming"),Gaming.IsChecked==true?"true":"false");}catch(IOException){}
    }
    internal static string ReadMode(string link)
    {
        if(!Uri.TryCreate(link.Trim(),UriKind.Absolute,out var uri)||uri.Scheme!="pingtunnel"||string.IsNullOrEmpty(uri.Host))throw new IOException("Paste a complete pingtunnel:// profile.");
        string mode="vpn";bool found=false;
        foreach(var field in uri.Query.TrimStart('?').Split('&',StringSplitOptions.RemoveEmptyEntries)){
            var pair=field.Split('=',2);
            if(Uri.UnescapeDataString(pair[0])!="mode")continue;
            if(found)throw new IOException("Duplicate mode in configuration.");found=true;
            mode=pair.Length==2?Uri.UnescapeDataString(pair[1]):"";
        }
        if(mode!="vpn"&&mode!="proxy")throw new IOException("Windows supports mode=vpn or mode=proxy; Android per-app profiles require exporting a Windows-compatible mode.");
        return mode;
    }
    void UpdateModes()
    {
        try { fullVpn=ReadMode(Link.Password)=="vpn";ProfileMode.Text=fullVpn?"mode=vpn · Full-device VPN":"mode=proxy · Local SOCKS5"; }
        catch(IOException){ProfileMode.Text="Mode follows the configuration";}
        if(!connected)Hint.Text = "The profile selects routing automatically. HTTPS relay delay is not raw ICMP or game ping.";
    }
    void PasteLink(object sender, RoutedEventArgs e)
    {
        try { if (Clipboard.ContainsText()) Link.Password = Clipboard.GetText().Trim(); } catch (System.Runtime.InteropServices.COMException) { Error.Text = "Clipboard busy. Try again."; }
    }
    bool SaveProfile()
    {
        profileSaver.Stop(); if(!profileDirty)return true;
        try { vault.Save(Link.Password);profileDirty=false;ProfileNote.Text=Link.Password.Length>0?"Saved · protected by your Windows account":"No saved link · paste one to get started";return true; }
        catch(Exception ex) when(ex is IOException or UnauthorizedAccessException or System.Security.Cryptography.CryptographicException)
        { ProfileNote.Text="Could not save this link. Keep a backup and try again.";return false; }
    }
    void ForgetProfile(object sender,RoutedEventArgs e)
    {
        if(connected||connecting)return;
        if(MessageBox.Show(this,"Remove the saved link from this Windows account? Your panel configuration is not deleted.","Forget connection",MessageBoxButton.YesNo,MessageBoxImage.Question)!=MessageBoxResult.Yes)return;
        Link.Password="";profileDirty=true;SaveProfile();
    }
    void OpenGithub(object sender, RoutedEventArgs e) => Process.Start(new ProcessStartInfo("https://github.com/namedkenzo/NamedClients") { UseShellExecute = true });
    static Process Start(string file, params string[] args)
    {
        var info = new ProcessStartInfo(Path.Combine(AppContext.BaseDirectory, file)) { UseShellExecute = false, CreateNoWindow = true, RedirectStandardInput = true, RedirectStandardOutput = true, RedirectStandardError = true, WorkingDirectory = AppContext.BaseDirectory };
        foreach (string arg in args) info.ArgumentList.Add(arg);
        return Process.Start(info) ?? throw new IOException("Could not start " + file);
    }
    async Task Watch(Process process, TaskCompletionSource<JsonElement> ready, CancellationToken ct)
    {
        // Errors intentionally exclude the profile and encryption key.
        var errors = process.StandardError.ReadToEndAsync(ct);
        try
        {
            while (await process.StandardOutput.ReadLineAsync(ct) is string line)
            {
                using var doc = JsonDocument.Parse(line);
                var root = doc.RootElement;
                string? kind = root.GetProperty("event").GetString();
                if (kind == "ready") ready.TrySetResult(root.Clone());
                if (kind == "stats") await Dispatcher.InvokeAsync(() => Traffic.Text = $"↑ {Bytes(root.GetProperty("upload").GetUInt64())}     ↓ {Bytes(root.GetProperty("download").GetUInt64())}");
            }
            await process.WaitForExitAsync(ct);
            if (!ct.IsCancellationRequested)
            {
                string detail = await errors;
                if (detail.Length > 350) detail = detail[..350];
                var error = new IOException(string.IsNullOrWhiteSpace(detail) ? "Connection process stopped." : detail.Trim());
                ready.TrySetException(error);
                if (!ct.IsCancellationRequested && connected && (process==engine||process==vpn)) await Dispatcher.InvokeAsync(async () => { Error.Text = error.Message; await Disconnect(); });
            }
        }
        catch (OperationCanceledException) { ready.TrySetCanceled(); }
        catch (Exception ex) { ready.TrySetException(ex); if (!ct.IsCancellationRequested && connected && (process==engine||process==vpn)) await Dispatcher.InvokeAsync(async () => { Error.Text = "Connection monitor stopped."; await Disconnect(); }); }
    }
    async void ToggleConnection(object sender, RoutedEventArgs e)
    {
        if (connected || connecting) { await Disconnect(); return; }
        if (string.IsNullOrWhiteSpace(Link.Password)) { Error.Text = "Paste the complete pingtunnel:// connection link."; return; }
        if(vpn!=null){Error.Text="Wait for VPN cleanup before reconnecting.";return;}
        try{fullVpn=ReadMode(Link.Password)=="vpn";}catch(IOException ex){Error.Text=ex.Message;return;}
        CancelTest();Error.Text = ""; connecting = true; session = new(); var ct = session.Token;
        Dog.State = "connecting"; StateLabel.Text = "●  Checking your tunnel…"; ConnectButton.Content = "Cancel";
        Gaming.IsEnabled = Link.IsEnabled = ForgetButton.IsEnabled = PasteButton.IsEnabled = TestButton.IsEnabled = false;
        try
        {
            engine = Start("namedp-engine.exe");
            var ready = new TaskCompletionSource<JsonElement>(TaskCreationOptions.RunContinuationsAsynchronously);
            _ = Watch(engine, ready, ct);
            await engine.StandardInput.WriteLineAsync(JsonSerializer.Serialize(new { link = Link.Password, gaming_mode=Gaming.IsChecked==true }));
            await engine.StandardInput.FlushAsync();
            JsonElement info = await ready.Task.WaitAsync(TimeSpan.FromSeconds(25), ct);
            if(info.GetProperty("mode").GetString()!=(fullVpn?"vpn":"proxy"))throw new IOException("Engine/profile routing mismatch.");
            string ip = info.GetProperty("server_ip").GetString()!;
            if (!IPAddress.TryParse(ip, out var address) || (address.AddressFamily != System.Net.Sockets.AddressFamily.InterNetwork && address.AddressFamily != System.Net.Sockets.AddressFamily.InterNetworkV6)) throw new IOException("Invalid tunnel endpoint address.");
            int port = info.GetProperty("port").GetInt32();
            if (fullVpn)
            {
                StateLabel.Text = "●  Preparing your private route…";
                string ps = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), @"WindowsPowerShell\v1.0\powershell.exe");
                vpn = Start(ps, "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", Path.Combine(AppContext.BaseDirectory, "vpn.ps1"), "-ParentPid", Environment.ProcessId.ToString(), "-EnginePid", engine.Id.ToString(), "-ServerIP", ip, "-Port", port.ToString());
                var vpnReady = new TaskCompletionSource<JsonElement>(TaskCreationOptions.RunContinuationsAsynchronously);
                _ = Watch(vpn, vpnReady, ct);
                await vpnReady.Task.WaitAsync(TimeSpan.FromSeconds(30), ct);
            }
            ct.ThrowIfCancellationRequested();
            connecting = false; connected = true; connectedAt = DateTime.UtcNow;
            Dog.State = "connected"; Dog.InvalidateVisual(); StateLabel.Text = "●  Connected";
            Headline.Text = "You're in good company.";
            Hint.Text = fullVpn ? "Full-device IPv4 VPN · TCP + UDP · IPv6 transport guarded" : $"SOCKS5 · 127.0.0.1:{port} · TCP + UDP";
            ConnectButton.Content = "Disconnect";
            TestButton.IsEnabled=true;
        }
        catch (OperationCanceledException) { }
        catch (Exception ex) { Error.Text = ex.Message; await Disconnect(); }
    }
    async Task Disconnect()
    {
        if (stopping) return; stopping = true;
        CancelTest();session?.Cancel(); connecting = connected = false;
        ConnectButton.IsEnabled = false;
        // EOF asks the VPN guardian to run its finally cleanup before exiting.
        if (vpn is { } v)
        {
            try { v.StandardInput.Close(); await v.WaitForExitAsync().WaitAsync(TimeSpan.FromSeconds(15)); }
            catch (Exception) { Error.Text = "VPN cleanup is still running. Keep namedP open until routes are restored."; }
            if (v.HasExited) { v.Dispose(); vpn = null; }
        }
        if (engine is { } p)
        {
            try { p.StandardInput.Close(); await p.WaitForExitAsync().WaitAsync(TimeSpan.FromSeconds(3)); }
            catch (Exception) { try { p.Kill(true); } catch (InvalidOperationException) { } }
            p.Dispose(); engine = null;
        }
        session?.Dispose(); session = null;
        Dog.State = "idle"; Dog.InvalidateVisual(); StateLabel.Text = "●  Ready when you are";
        Headline.Text = "Your network, a little calmer."; ConnectButton.Content = "Connect →";
        Gaming.IsEnabled = Link.IsEnabled = ForgetButton.IsEnabled = PasteButton.IsEnabled = ConnectButton.IsEnabled = TestButton.IsEnabled = vpn == null;
        UpdateModes(); stopping = false;
    }
    async void OnClosing(object? sender, CancelEventArgs e)
    {
        if (closing) return; e.Cancel = true;
        if(closePending)return;
        if(!SaveProfile() && MessageBox.Show(this,"This link could not be saved. Close anyway? Keep a backup before closing.","Link not saved",MessageBoxButton.YesNo,MessageBoxImage.Warning)!=MessageBoxResult.Yes)return;
        closePending=true;
        Link.IsEnabled=PasteButton.IsEnabled=ForgetButton.IsEnabled=TestButton.IsEnabled=false;
        // Let the first Closing event return before calling Close again, even
        // when disconnected cleanup completes synchronously.
        await System.Windows.Threading.Dispatcher.Yield(DispatcherPriority.Background);
        await Disconnect();closePending=false;
        if (vpn != null) return;
        closing = true; ticker.Stop(); profileSaver.Stop(); SystemEvents.UserPreferenceChanged -= SystemThemeChanged; Close();
    }
    static string Bytes(ulong n) => n < 1024 ? $"{n} B" : n < 1048576 ? $"{n / 1024d:F1} KiB" : $"{n / 1048576d:F1} MiB";
    void CancelTest(){testSession?.Cancel();try{testEngine?.Kill(true);}catch(InvalidOperationException){} }
    async void TestDelay(object sender,RoutedEventArgs e)
    {
        if(testing||connecting||closing)return;
        try{ReadMode(Link.Password);}catch(IOException ex){Delay.Text=ex.Message;return;}
        string testedLink=Link.Password;testing=true;TestButton.IsEnabled=false;TestButton.Content="Testing…";
        Delay.Text="Testing HTTPS through this profile…";
        using var cancellation=new CancellationTokenSource(TimeSpan.FromSeconds(30));testSession=cancellation;
        Process? process=null;
        try{
            process=Start("namedp-engine.exe");testEngine=process;
            var stdout=process.StandardOutput.ReadToEndAsync(cancellation.Token);
            var stderr=process.StandardError.ReadToEndAsync(cancellation.Token);
            await process.StandardInput.WriteLineAsync(JsonSerializer.Serialize(new{link=testedLink,gaming_mode=Gaming.IsChecked==true,relay_delay_only=true}));await process.StandardInput.FlushAsync();
            await process.WaitForExitAsync(cancellation.Token);
            if(process.ExitCode!=0)throw new IOException("Relay test failed. Check the profile, outbound and HTTPS reachability.");
            using var doc=JsonDocument.Parse(await stdout);var result=doc.RootElement;
            long ms=ReadRelayDelay(result);
            if(testedLink==Link.Password)Delay.Text=$"{ms} ms · HTTPS 204";
        }catch(OperationCanceledException){Delay.Text="Relay test cancelled or timed out.";}
        catch(Exception){if(testedLink==Link.Password)Delay.Text="Relay test failed; the VPN session is unchanged.";}
        finally{
            try{if(process!=null&&!process.HasExited)process.Kill(true);}catch(InvalidOperationException){}
            process?.Dispose();if(testEngine==process)testEngine=null;testSession=null;
            testing=false;TestButton.Content="Test delay";TestButton.IsEnabled=!connecting&&!closing&&(vpn==null||connected);
        }
    }
    internal static long ReadRelayDelay(JsonElement result){
        long ms=result.GetProperty("latency_ms").GetInt64();
        if(result.GetProperty("event").GetString()!="relay_delay"||result.GetProperty("url").GetString()!="https://www.gstatic.com/generate_204"||result.GetProperty("status").GetInt32()!=204||ms<1||ms>15000)throw new IOException("Invalid relay test response.");
        return ms;
    }
}

