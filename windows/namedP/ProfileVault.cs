using System.IO;
using System.Security.Cryptography;
using System.Text;

namespace NamedP;

// Only ciphertext ever reaches disk, including the atomic replacement file.
// CurrentUser binds the secret to this Windows account, not to the install folder.
public sealed class ProfileVault(string path)
{
    static readonly byte[] entropy = Encoding.UTF8.GetBytes("namedP.saved-profile.v1");
    const int MaxBytes = 65536;
    public string Load()
    {
        if (!File.Exists(path)) return "";
        if (new FileInfo(path).Length > MaxBytes + 4096) throw new IOException("Saved profile is too large.");
        byte[] ciphertext = File.ReadAllBytes(path);
        byte[] cleartext = ProtectedData.Unprotect(ciphertext, entropy, DataProtectionScope.CurrentUser);
        try
        {
            if (cleartext.Length > MaxBytes) throw new IOException("Saved profile is too large.");
            return Encoding.UTF8.GetString(cleartext);
        }
        finally { CryptographicOperations.ZeroMemory(cleartext); }
    }
    public void Save(string link)
    {
        if (string.IsNullOrWhiteSpace(link)) { File.Delete(path); return; }
        if (Encoding.UTF8.GetByteCount(link) > MaxBytes) throw new IOException("Profile is too large to save.");
        byte[] cleartext = Encoding.UTF8.GetBytes(link);
        byte[] ciphertext;
        try { ciphertext = ProtectedData.Protect(cleartext, entropy, DataProtectionScope.CurrentUser); }
        finally { CryptographicOperations.ZeroMemory(cleartext); }
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        string temporary = path + "." + Guid.NewGuid().ToString("N") + ".tmp";
        try
        {
            using (var stream = new FileStream(temporary, FileMode.CreateNew, FileAccess.Write, FileShare.None, 4096, FileOptions.WriteThrough))
            { stream.Write(ciphertext); stream.Flush(true); }
            File.Move(temporary, path, overwrite: true);
        }
        finally { if (File.Exists(temporary)) File.Delete(temporary); }
    }
}
