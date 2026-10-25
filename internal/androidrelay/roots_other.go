//go:build !android

package androidrelay

import "crypto/tls"

func deviceTLSConfig() (*tls.Config, error) { return nil, nil }
