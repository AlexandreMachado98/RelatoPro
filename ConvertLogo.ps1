Add-Type -AssemblyName System.Drawing
$source = "C:\Users\alexa\.gemini\antigravity\brain\04c2cee3-e97f-4317-ac99-86ad1f117a02\relatopro_logo_1788092210630.jpg"
$img = [System.Drawing.Image]::FromFile($source)
$img.Save("C:\Users\alexa\Desktop\NOVO PROJETO\app\src\main\res\mipmap-xxxhdpi\ic_launcher.png", [System.Drawing.Imaging.ImageFormat]::Png)
$img.Save("C:\Users\alexa\Desktop\NOVO PROJETO\app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.png", [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "Done"
