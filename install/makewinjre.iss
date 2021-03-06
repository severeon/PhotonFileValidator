; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "Photon File Validator"
#define MyAppVersion "2.2"
#define MyAppPublisher "Bonosoft"
#define MyAppURL "https://github.com/Photonsters/PhotonFileValidator"
#define MyAppExeName "photonfilevalidator.exe"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{DF9C6F8D-CAD2-4BBB-8BD3-CB72B41FF2BC}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf}\{#MyAppName}
DisableProgramGroupPage=yes
OutputDir=D:\Users\bn\Documents\GitHub\PhotonFileViewer\release
OutputBaseFilename=win-jre-photonfilevalidator-{#MyAppVersion}
SetupIconFile=D:\Users\bn\Documents\GitHub\PhotonFileViewer\install\img\icons\validator.ico
Compression=lzma
SolidCompression=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "D:\Users\bn\Documents\GitHub\PhotonFileViewer\out\win\photonfilevalidator.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\PhotonFileViewer\out\win\photon.properties"; DestDir: "{app}"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\*"; DestDir: "{app}/jre"; Flags: ignoreversion

Source: "D:\Users\bn\Documents\GitHub\jre\bin\*"; DestDir: "{app}\jre\bin"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\bin\dtplugin\*"; DestDir: "{app}\jre\bin\dtplugin"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\bin\plugin2\*"; DestDir: "{app}\jre\bin\plugin2"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\bin\server\*"; DestDir: "{app}\jre\bin\server"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\*"; DestDir: "{app}\jre\lib"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\amd64\*"; DestDir: "{app}\jre\lib\amd64"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\cmm\*"; DestDir: "{app}\jre\lib\cmm"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\deploy\*"; DestDir: "{app}\jre\lib\deploy"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\ext\*"; DestDir: "{app}\jre\lib\ext"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\fonts\*"; DestDir: "{app}\jre\lib\fonts"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\jfr\*"; DestDir: "{app}\jre\lib\jfr"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\management\*"; DestDir: "{app}\jre\lib\management"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\security\*"; DestDir: "{app}\jre\lib\security"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\images\cursors\*"; DestDir: "{app}\jre\lib\images\cursors"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\security\policy\limited\*"; DestDir: "{app}\jre\lib\security\policy\limited"; Flags: ignoreversion
Source: "D:\Users\bn\Documents\GitHub\jre\lib\security\policy\unlimited\*"; DestDir: "{app}\jre\lib\security\policy\unlimited"; Flags: ignoreversion

; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{commonprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

