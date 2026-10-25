using System.Windows;
namespace NamedP;
public partial class App : Application {
    void ButtonPressed(object sender,System.Windows.Input.MouseButtonEventArgs e){AnimateButton(sender,0.97,90);}
    void ButtonReleased(object sender,System.Windows.Input.MouseButtonEventArgs e){AnimateButton(sender,1,170);}
    void ButtonUncaptured(object sender,System.Windows.Input.MouseEventArgs e){AnimateButton(sender,1,170);}
    static void AnimateButton(object sender,double scale,int duration){
        if(!SystemParameters.ClientAreaAnimation||sender is not System.Windows.Controls.Button b)return;
        if(b.RenderTransform is not System.Windows.Media.ScaleTransform){b.RenderTransform=new System.Windows.Media.ScaleTransform(1,1);b.RenderTransformOrigin=new Point(.5,.5);}
        var transform=(System.Windows.Media.ScaleTransform)b.RenderTransform;
        var animation=new System.Windows.Media.Animation.DoubleAnimation(scale,TimeSpan.FromMilliseconds(duration)){EasingFunction=new System.Windows.Media.Animation.QuadraticEase{EasingMode=System.Windows.Media.Animation.EasingMode.EaseOut}};
        transform.BeginAnimation(System.Windows.Media.ScaleTransform.ScaleXProperty,animation);transform.BeginAnimation(System.Windows.Media.ScaleTransform.ScaleYProperty,animation);
    }
    System.Threading.Mutex? instance;
    bool owned;
    protected override void OnStartup(StartupEventArgs e){
        try{instance=new System.Threading.Mutex(true,@"Global\namedP-Tunnel-UI",out owned);}
        catch(UnauthorizedAccessException){MessageBox.Show("namedP is already running. Close it before starting another copy.");Shutdown();return;}
        if(!owned){MessageBox.Show("namedP is already running. Close it before starting another copy.");Shutdown();return;}
        base.OnStartup(e);
    }
    protected override void OnExit(ExitEventArgs e){if(owned)instance?.ReleaseMutex();instance?.Dispose();base.OnExit(e);}
}

