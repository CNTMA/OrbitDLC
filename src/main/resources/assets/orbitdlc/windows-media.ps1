$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing
$asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.IsGenericMethodDefinition -and $_.GetParameters().Count -eq 1 -and
    $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
} | Select-Object -First 1

function Wait-WinRt($operation, [type]$resultType) {
    $task = $asTask.MakeGenericMethod($resultType).Invoke($null, @($operation))
    if (!$task.Wait(2500)) { throw 'Media operation timed out' }
    return $task.Result
}

$managerType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
$propertiesType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType=WindowsRuntime]
$streamType = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType=WindowsRuntime]

function Get-Cover($thumbnail) {
    if ($null -eq $thumbnail) { return $null }
    $stream = Wait-WinRt ($thumbnail.OpenReadAsync()) $streamType
    try {
        if ($stream.Size -gt 4194304) { return $null }
        $imageStream = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($stream.GetInputStreamAt([uint64]0))
        try {
            $image = [Drawing.Image]::FromStream($imageStream)
            try {
                if ($image.Width -gt 4096 -or $image.Height -gt 4096) { return $null }
                $cover = [Drawing.Bitmap]::new(96, 96)
                $output = [IO.MemoryStream]::new()
                try {
                    $graphics = [Drawing.Graphics]::FromImage($cover)
                    try {
                        $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                        $side = [Math]::Min($image.Width, $image.Height)
                        $sourceX = [int](($image.Width - $side) / 2)
                        $sourceY = [int](($image.Height - $side) / 2)
                        $graphics.DrawImage($image, [Drawing.Rectangle]::new(0, 0, 96, 96),
                            $sourceX, $sourceY, $side, $side, [Drawing.GraphicsUnit]::Pixel)
                    } finally { $graphics.Dispose() }
                    $cover.Save($output, [Drawing.Imaging.ImageFormat]::Png)
                    return [Convert]::ToBase64String($output.ToArray())
                } finally { $cover.Dispose(); $output.Dispose() }
            } finally { $image.Dispose() }
        } finally { $imageStream.Dispose() }
    } finally { $stream.Dispose() }
}

$coverKey = $null
$coverData = $null
$manager = Wait-WinRt ($managerType::RequestAsync()) $managerType
$session = $null
while ($null -ne ($command = [Console]::ReadLine())) {
    try {
        if ($command -eq 'status') {
            $session = $manager.GetCurrentSession()
            if ($null -eq $session -or $session.GetPlaybackInfo().PlaybackStatus -ne 'Playing') {
                foreach ($candidate in $manager.GetSessions()) {
                    if ($candidate.GetPlaybackInfo().PlaybackStatus -eq 'Playing') {
                        $session = $candidate
                        break
                    }
                }
            }
        } elseif ($null -ne $session) {
            switch ($command) {
                'previous' { $null = Wait-WinRt ($session.TrySkipPreviousAsync()) ([bool]) }
                'toggle' { $null = Wait-WinRt ($session.TryTogglePlayPauseAsync()) ([bool]) }
                'next' { $null = Wait-WinRt ($session.TrySkipNextAsync()) ([bool]) }
            }
        }
        if ($null -eq $session) { $coverKey = $null; $coverData = $null; [Console]::WriteLine('{}'); continue }
        $properties = Wait-WinRt ($session.TryGetMediaPropertiesAsync()) $propertiesType
        $key = ConvertTo-Json -Compress -InputObject @($session.SourceAppUserModelId,
            $properties.Title, $properties.Artist, $properties.AlbumTitle, ($null -ne $properties.Thumbnail))
        if ($coverKey -cne $key) {
            $coverRetryAt = [DateTime]::MinValue; $coverData = $null
            $coverKey = $key
        }
        if (!$coverData -and $null -ne $properties.Thumbnail -and [DateTime]::UtcNow -ge $coverRetryAt) {
            $coverRetryAt = [DateTime]::UtcNow.AddSeconds(10)
            try { $coverData = Get-Cover $properties.Thumbnail } catch {
                [Console]::Error.WriteLine($_.Exception.GetBaseException().Message)
            }
        }
        $info = $session.GetPlaybackInfo()
        $controls = $info.Controls
        [Console]::WriteLine((@{
            title = $properties.Title
            artist = $properties.Artist
            cover = $coverData
            playing = $info.PlaybackStatus -eq 'Playing'
            previous = $controls.IsPreviousEnabled
            toggle = $controls.IsPlayPauseToggleEnabled
            next = $controls.IsNextEnabled
        } | ConvertTo-Json -Compress))
    } catch {
        [Console]::WriteLine('{"error":true}')
    }
}
