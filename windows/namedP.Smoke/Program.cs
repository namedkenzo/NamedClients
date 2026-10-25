using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using NamedP;

static class Program
{
 [STAThread] static int Main()
 {
  var app = new App(); app.InitializeComponent();app.ShutdownMode=ShutdownMode.OnExplicitShutdown;
  string data=Path.Combine(Path.GetTempPath(),"namedP-vault-tests-"+Guid.NewGuid().ToString("N"));
  Directory.CreateDirectory(data);
  var vault=new ProfileVault(Path.Combine(data,"profile.dat"));
  string saved="pingtunnel://restart.example?mode=proxy&encrypt=chacha20&encrypt_key=private-key%2Bvalue";
  if(vault.Load()!="")throw new Exception("Missing profile must be empty");
  vault.Save(saved);
  if(System.Text.Encoding.UTF8.GetString(File.ReadAllBytes(Path.Combine(data,"profile.dat"))).Contains("restart.example"))throw new Exception("Plaintext leaked to disk");
  var window = new MainWindow(vault); window.Show();
  if(((PasswordBox)window.FindName("Link")).Password!=saved)throw new Exception("Saved profile not restored into UI");
  ((PasswordBox)window.FindName("Link")).Password="pingtunnel://closed.example?mode=vpn&key=123456";
  window.Close(); // Closing must flush the pending debounce, not lose the edit.
  window=new MainWindow(vault);window.Show();
  if(((PasswordBox)window.FindName("Link")).Password!="pingtunnel://closed.example?mode=vpn&key=123456")throw new Exception("Profile lost on restart");
  ((PasswordBox)window.FindName("Link")).Password="";
  window.Close();if(vault.Load()!=""||File.Exists(Path.Combine(data,"profile.dat")))throw new Exception("Cleared profile persisted");
  File.WriteAllBytes(Path.Combine(data,"profile.dat"),new byte[]{1,2,3});
  window=new MainWindow(vault);window.Show();window.Close();
  if(!File.ReadAllBytes(Path.Combine(data,"profile.dat")).SequenceEqual(new byte[]{1,2,3}))throw new Exception("Unreadable saved data silently overwritten");
  vault.Save("");
  window=new MainWindow(vault);window.Show();
  var theme = (ComboBox)window.FindName("Theme");
  Directory.CreateDirectory("windows/namedP.Smoke");
  foreach (var pair in new[] { (1,"dark"),(2,"light") })
  {
   theme.SelectedIndex = pair.Item1;
   window.Measure(new Size(540,870)); window.Arrange(new Rect(0,0,540,870)); window.UpdateLayout();
   theme.IsDropDownOpen=true;window.UpdateLayout();theme.IsDropDownOpen=false;
   var image = new RenderTargetBitmap(540,870,96,96,PixelFormats.Pbgra32); image.Render(window);
   var png = new PngBitmapEncoder(); png.Frames.Add(BitmapFrame.Create(image));
   using var file = File.Create("windows/namedP.Smoke/preview-"+pair.Item2+".png"); png.Save(file);
  }
  var dog = (Dachshund)window.FindName("Dog");
  if(window.FindName("VpnMode")!=null||window.FindName("ProxyMode")!=null)throw new Exception("Obsolete manual routing modes remain");
  var link=(PasswordBox)window.FindName("Link");
  link.Password="pingtunnel://example.com?key=123&mode=proxy";
  if(!((TextBlock)window.FindName("ProfileMode")).Text.Contains("mode=proxy"))throw new Exception("Proxy URI mode lost");
  link.Password="pingtunnel://example.com?key=123&mode=vpn";
  if(!((TextBlock)window.FindName("ProfileMode")).Text.Contains("mode=vpn"))throw new Exception("VPN URI mode lost");
  if(window.FindName("Gaming")==null||window.FindName("TestButton")==null)throw new Exception("Gaming/delay controls missing");
  link.Password="";
  var flags=System.Reflection.BindingFlags.Static|System.Reflection.BindingFlags.NonPublic;
  var mode=typeof(MainWindow).GetMethod("ReadMode",flags)!;
  if((string)mode.Invoke(null,new object[]{"pingtunnel://example.com?key=123"})! != "vpn")throw new Exception("Default mode mismatch");
  foreach(var uri in new[]{"pingtunnel://example.com?mode=other","pingtunnel://example.com?mode=vpn&mode=proxy"}){
   try{mode.Invoke(null,new object[]{uri});throw new Exception("Invalid mode accepted");}catch(System.Reflection.TargetInvocationException ex) when(ex.InnerException is IOException){}
  }
  var delay=typeof(MainWindow).GetMethod("ReadRelayDelay",flags)!;
  foreach(var values in new[]{("relay_delay",204,125),("probe_ok",204,125),("relay_delay",200,125),("relay_delay",204,-1)}){
   using var doc=System.Text.Json.JsonDocument.Parse(System.Text.Json.JsonSerializer.Serialize(new{ @event=values.Item1,status=values.Item2,latency_ms=values.Item3,url="https://www.gstatic.com/generate_204"}));
   bool valid=values.Item1=="relay_delay"&&values.Item2==204&&values.Item3>0;
   try{var ms=(long)delay.Invoke(null,new object[]{doc.RootElement})!;if(!valid||ms!=125)throw new Exception("Invalid delay accepted");}
   catch(System.Reflection.TargetInvocationException ex) when(ex.InnerException is IOException){if(valid)throw;}
  }
  foreach (string state in new[]{"idle","connecting","connected"}) { dog.State=state; dog.InvalidateVisual(); window.UpdateLayout(); }
  window.Close();vault.Save("");Directory.Delete(data); // Only our uniquely created empty test directory.
  Console.WriteLine("Encrypted profile restart, immediate-close flush, deletion, corruption protection, polished themes, mode, delay and mascot passed.");
  return 0;
 }
}

