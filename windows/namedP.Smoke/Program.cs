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
  var app = new App(); app.InitializeComponent();
  var window = new MainWindow(); window.Show();
  var theme = (ComboBox)window.FindName("Theme");
  Directory.CreateDirectory("windows/namedP.Smoke");
  foreach (var pair in new[] { (1,"dark"),(2,"light") })
  {
   theme.SelectedIndex = pair.Item1;
   window.Measure(new Size(530,800)); window.Arrange(new Rect(0,0,530,800)); window.UpdateLayout();
   var image = new RenderTargetBitmap(530,800,96,96,PixelFormats.Pbgra32); image.Render(window);
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
  foreach (string state in new[]{"idle","connecting","connected"}) { dog.State=state; dog.InvalidateVisual(); window.UpdateLayout(); }
  Console.WriteLine("Window, system/dark/light themes and mascot rendered successfully.");
  return 0;
 }
}

