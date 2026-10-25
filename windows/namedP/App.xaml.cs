using System.Windows;
namespace NamedP;
public partial class App : Application {
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

