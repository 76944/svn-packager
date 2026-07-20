[Setup]
AppName=SVN增量打包工具
AppVersion=1.0.0
DefaultDirName={pf}\SVNPackager
DefaultGroupName=SVN工具
UninstallDisplayIcon={app}\SVNPackager.exe
Compression=lzma2
SolidCompression=yes
OutputDir=installer
OutputBaseFilename=SVNPackager_Setup
SetupIconFile=icon.ico

[Files]
Source: "target\SVNPackager.exe"; DestDir: "{app}"
Source: "README.txt"; DestDir: "{app}"

[Code]
function IsJavaInstalled: Boolean;
var
  ResultCode: Integer;
begin
  Result := Exec('cmd.exe', '/c java -version', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) and (ResultCode = 0);
end;

function InitializeSetup: Boolean;
begin
  Result := True;
  if not IsJavaInstalled then
  begin
    if MsgBox('未检测到Java运行时环境（需要JDK/JRE 1.8+）。是否继续安装？', mbConfirmation, MB_YESNO) = IDNO then
      Result := False;
  end;
end;

[Icons]
Name: "{group}\SVN增量打包工具"; Filename: "{app}\SVNPackager.exe"
Name: "{group}\卸载"; Filename: "{uninstallexe}"
Name: "{commondesktop}\SVN增量打包工具"; Filename: "{app}\SVNPackager.exe"

[Run]
Filename: "{app}\SVNPackager.exe"; Description: "启动SVN增量打包工具"; Flags: postinstall nowait skipifsilent
