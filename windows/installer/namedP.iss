#ifndef ClientVersion
 #define ClientVersion "0.3.0-alpha"
#endif
[Setup]
AppId={{C9D683F3-BA6C-4418-8D45-E9492133B742}
AppName=namedP
AppVersion={#ClientVersion}
AppPublisher=namedkenzo
AppPublisherURL=https://github.com/namedkenzo/NamedClients
DefaultDirName={autopf}\namedP
DefaultGroupName=namedP
DisableDirPage=no
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
AppMutex=Global\namedP-Tunnel-UI
CloseApplications=no
OutputDir=..\..\dist
OutputBaseFilename=namedP-{#ClientVersion}-windows-x64-setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
UninstallDisplayIcon={app}\namedP.exe
LicenseFile=..\..\LICENSE
[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; Flags: unchecked
[Files]
Source: "..\..\dist\namedP\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
[Icons]
Name: "{group}\namedP"; Filename: "{app}\namedP.exe"
Name: "{autodesktop}\namedP"; Filename: "{app}\namedP.exe"; Tasks: desktopicon
[UninstallDelete]
; Session routes are cleaned by the running guardian, never by an uninstaller.
Type: files; Name: "{app}\*.log"
