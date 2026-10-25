# namedP Android 0.2.2-alpha — optional gaming policy and lighter motion

- Settings → Gaming enables shorter 32-packet UDP association queues. Stale
  association packets older than 150 ms are dropped under load. Reconnect to apply.
  TCP ordering/reliability and the default non-gaming policy are unchanged.
- This trades delayed UDP for potential packet loss; it is not congestion control
  or a guarantee of lower ping. Use Direct/UDP-capable SOCKS outbounds, pause bulk
  downloads and optionally select only your game using per-app VPN.
- NamedICMP 0.4.2 separates UDP replies from bulk TCP queues. Existing panels
  remain compatible, but updating the panel enables that scheduling improvement.
- Mascot drawing reuses paths/colors, runs at 20 FPS idle / about 30 FPS active,
  stops scheduling while hidden and honors disabled system animations.
- Buttons use a subtle 90 ms press and 170 ms release transition. Dashboard
  refresh is once per second. No game traffic interception or payload changes.
- Windows binaries, version and releases are unchanged. Shared client code keeps
  the legacy queue policy unless Android explicitly enables Gaming.

Still a development-signed alpha. Automated regression tests do not replace
real-game, phone and carrier jitter/loss testing. HTTPS relay delay is not game ping.
