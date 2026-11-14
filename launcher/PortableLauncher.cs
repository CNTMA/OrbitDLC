using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Security.Cryptography;
using System.Threading;
using System.Windows.Forms;

internal static class PortableLauncher
{
    [STAThread]
    private static void Main()
    {
        try
        {
            string local = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "OrbitVisuals");
            string runtime = InstallRuntime(Assembly.GetExecutingAssembly().Location, local);
            var launch = new ProcessStartInfo(Path.Combine(runtime, "OrbitVisuals.exe"));
            launch.WorkingDirectory = runtime;
            launch.UseShellExecute = false;
            launch.EnvironmentVariables["ORBITVISUALS_DATA"] = Path.Combine(local, "data");
            Process.Start(launch);
        }
        catch (Exception error)
        {
            MessageBox.Show(error.Message, "OrbitVisuals", MessageBoxButtons.OK, MessageBoxIcon.Error);
            Environment.ExitCode = 1;
        }
    }

    private static string InstallRuntime(string executable, string local)
    {
        using (var source = File.OpenRead(executable))
        using (var sha = SHA256.Create())
        {
            string id = BitConverter.ToString(sha.ComputeHash(source)).Replace("-", "").Substring(0, 24);
            string runtime = Path.Combine(local, "runtime", id);
            string marker = Path.Combine(runtime, "OrbitVisuals.exe");
            using (var mutex = new Mutex(false, "Local\\OrbitVisuals-" + id))
            {
                try { mutex.WaitOne(); }
                catch (AbandonedMutexException) { }
                try
                {
                    if (!File.Exists(marker)) Extract(source, runtime);
                    return runtime;
                }
                finally { mutex.ReleaseMutex(); }
            }
        }
    }

    private static void Extract(FileStream source, string runtime)
    {
        source.Seek(-8, SeekOrigin.End);
        long length = new BinaryReader(source).ReadInt64();
        long offset = source.Length - 8 - length;
        if (length <= 0 || offset <= 0) throw new InvalidDataException("Invalid OrbitVisuals package.");

        string staging = runtime + "." + Guid.NewGuid().ToString("N");
        Directory.CreateDirectory(staging);
        try
        {
            string archivePath = Path.Combine(staging, "runtime.zip");
            source.Position = offset;
            using (var archive = File.Create(archivePath))
            {
                var buffer = new byte[1024 * 1024];
                while (length > 0)
                {
                    int count = source.Read(buffer, 0, (int)Math.Min(buffer.Length, length));
                    if (count == 0) throw new EndOfStreamException();
                    archive.Write(buffer, 0, count);
                    length -= count;
                }
            }
            string extracted = Path.Combine(staging, "app");
            ZipFile.ExtractToDirectory(archivePath, extracted);
            if (!File.Exists(Path.Combine(extracted, "OrbitVisuals.exe")))
                throw new InvalidDataException("OrbitVisuals.exe is missing from the package.");
            Directory.Move(extracted, runtime);
        }
        finally { Directory.Delete(staging, true); }
    }
}
