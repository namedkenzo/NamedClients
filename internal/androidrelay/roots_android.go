package androidrelay

import (
	"crypto/tls"
	"crypto/x509"
	"errors"
	"io"
	"os"
	"path/filepath"
)

func deviceTLSConfig() (*tls.Config, error) {
	roots, _ := x509.SystemCertPool()
	if roots == nil { roots = x509.NewCertPool() }
	// Android 14+ keeps updatable system roots in the Conscrypt APEX. The Go
	// default may only inspect the legacy system directory. Never disable TLS
	// verification or load a caller-supplied/downloaded certificate bundle.
	for _, directory := range []string{"/apex/com.android.conscrypt/cacerts", "/system/etc/security/cacerts"} {
		entries, err := os.ReadDir(directory)
		if err != nil { continue }
		for _, entry := range entries {
			if entry.IsDir() { continue }
			file, err := os.Open(filepath.Join(directory, entry.Name()))
			if err != nil { continue }
			pem, err := io.ReadAll(io.LimitReader(file, 65537)); file.Close()
			if err == nil && len(pem) <= 65536 { roots.AppendCertsFromPEM(pem) }
		}
	}
	if len(roots.Subjects()) == 0 { return nil, errors.New("Android system TLS trust store is unavailable") }
	return &tls.Config{RootCAs: roots, MinVersion: tls.VersionTLS12}, nil
}
