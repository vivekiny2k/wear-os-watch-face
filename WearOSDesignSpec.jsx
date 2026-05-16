import { useState } from "react";

const colors = {
  bg: "#0a0e1a",
  surface: "#111827",
  surfaceHigh: "#1a2235",
  border: "#1e2d45",
  accent: "#00d4ff",
  accentDim: "#0097b8",
  accentGlow: "rgba(0,212,255,0.15)",
  green: "#00e5a0",
  amber: "#ffb300",
  red: "#ff4f4f",
  purple: "#a78bfa",
  text: "#e2e8f0",
  textDim: "#7a90b0",
  textMuted: "#3d5070",
};

const sections = [
  "Overview",
  "Architecture",
  "Watch Face",
  "Ambient Mode",
  "Action Panels",
  "Data & Networking",
  "Phone Communication",
  "Project Structure",
  "Tech Stack",
];

const Badge = ({ color, children }) => (
  <span style={{
    display: "inline-block",
    padding: "2px 10px",
    borderRadius: 20,
    fontSize: 11,
    fontWeight: 700,
    letterSpacing: "0.08em",
    background: color + "22",
    color: color,
    border: `1px solid ${color}44`,
    fontFamily: "monospace",
  }}>{children}</span>
);

const Tag = ({ children }) => (
  <span style={{
    display: "inline-block",
    padding: "1px 8px",
    borderRadius: 4,
    fontSize: 11,
    background: colors.surfaceHigh,
    color: colors.textDim,
    border: `1px solid ${colors.border}`,
    fontFamily: "monospace",
    marginRight: 4,
    marginBottom: 4,
  }}>{children}</span>
);

const SectionTitle = ({ children }) => (
  <h2 style={{
    fontFamily: "'Courier New', monospace",
    fontSize: 13,
    fontWeight: 700,
    letterSpacing: "0.2em",
    textTransform: "uppercase",
    color: colors.accent,
    margin: "0 0 24px 0",
    paddingBottom: 12,
    borderBottom: `1px solid ${colors.border}`,
    display: "flex",
    alignItems: "center",
    gap: 10,
  }}>
    <span style={{ color: colors.accentDim }}>// </span>{children}
  </h2>
);

const SubTitle = ({ children }) => (
  <h3 style={{
    fontFamily: "'Courier New', monospace",
    fontSize: 11,
    fontWeight: 700,
    letterSpacing: "0.15em",
    textTransform: "uppercase",
    color: colors.purple,
    margin: "28px 0 12px 0",
  }}>{children}</h3>
);

const Card = ({ children, accent }) => (
  <div style={{
    background: colors.surfaceHigh,
    border: `1px solid ${accent ? accent + "44" : colors.border}`,
    borderLeft: `3px solid ${accent || colors.border}`,
    borderRadius: 8,
    padding: "16px 20px",
    marginBottom: 12,
  }}>{children}</div>
);

const Row = ({ label, value, valueColor }) => (
  <div style={{
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    padding: "8px 0",
    borderBottom: `1px solid ${colors.border}`,
    gap: 16,
  }}>
    <span style={{ color: colors.textDim, fontSize: 13, minWidth: 160, fontFamily: "monospace" }}>{label}</span>
    <span style={{ color: valueColor || colors.text, fontSize: 13, textAlign: "right", flex: 1 }}>{value}</span>
  </div>
);

const CodeBlock = ({ children }) => (
  <pre style={{
    background: "#070c18",
    border: `1px solid ${colors.border}`,
    borderRadius: 6,
    padding: "14px 16px",
    fontSize: 12,
    fontFamily: "'Courier New', monospace",
    color: colors.text,
    overflowX: "auto",
    margin: "12px 0",
    lineHeight: 1.7,
  }}>{children}</pre>
);

const ApiItem = ({ method, path, desc, dir }) => (
  <div style={{
    display: "flex",
    alignItems: "flex-start",
    gap: 10,
    padding: "10px 0",
    borderBottom: `1px solid ${colors.border}`,
  }}>
    <Badge color={dir === "→" ? colors.amber : dir === "←" ? colors.green : colors.accent}>{method}</Badge>
    <div>
      <code style={{ color: colors.accent, fontSize: 12, fontFamily: "monospace" }}>{path}</code>
      <div style={{ color: colors.textDim, fontSize: 12, marginTop: 3 }}>{desc}</div>
    </div>
  </div>
);

const WatchFaceZone = ({ label, x, y, w, h, color, note }) => (
  <div style={{
    position: "absolute",
    left: `${x}%`, top: `${y}%`,
    width: `${w}%`, height: `${h}%`,
    border: `1.5px dashed ${color}88`,
    background: color + "18",
    borderRadius: 4,
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    cursor: "default",
  }}>
    <span style={{ fontSize: 9, fontWeight: 700, color, fontFamily: "monospace", textAlign: "center", lineHeight: 1.3, padding: "0 4px" }}>{label}</span>
    {note && <span style={{ fontSize: 8, color: color + "aa", textAlign: "center", padding: "0 4px" }}>{note}</span>}
  </div>
);

export default function DesignSpec() {
  const [active, setActive] = useState("Overview");

  return (
    <div style={{
      minHeight: "100vh",
      background: colors.bg,
      color: colors.text,
      fontFamily: "'Georgia', serif",
      display: "flex",
    }}>
      {/* Sidebar */}
      <div style={{
        width: 200,
        minHeight: "100vh",
        background: colors.surface,
        borderRight: `1px solid ${colors.border}`,
        padding: "24px 0",
        position: "sticky",
        top: 0,
        flexShrink: 0,
      }}>
        <div style={{ padding: "0 20px 24px", borderBottom: `1px solid ${colors.border}` }}>
          <div style={{ fontSize: 10, letterSpacing: "0.2em", color: colors.textMuted, fontFamily: "monospace", marginBottom: 4 }}>DESIGN SPEC</div>
          <div style={{ fontSize: 14, fontWeight: 700, color: colors.accent, fontFamily: "monospace" }}>WearOS WatchApp</div>
          <div style={{ fontSize: 10, color: colors.textDim, marginTop: 4 }}>Galaxy Watch 6 · LTE</div>
        </div>
        <nav style={{ padding: "16px 0" }}>
          {sections.map(s => (
            <button key={s} onClick={() => setActive(s)} style={{
              display: "block",
              width: "100%",
              textAlign: "left",
              padding: "9px 20px",
              background: active === s ? colors.accentGlow : "transparent",
              borderLeft: active === s ? `3px solid ${colors.accent}` : "3px solid transparent",
              border: "none",
              color: active === s ? colors.accent : colors.textDim,
              fontSize: 12,
              fontFamily: "monospace",
              cursor: "pointer",
              letterSpacing: "0.05em",
              transition: "all 0.15s",
            }}>{s}</button>
          ))}
        </nav>
        <div style={{ padding: "16px 20px", borderTop: `1px solid ${colors.border}`, marginTop: 8 }}>
          <div style={{ fontSize: 10, color: colors.textMuted, fontFamily: "monospace" }}>v1.0 · May 2026</div>
        </div>
      </div>

      {/* Main content */}
      <div style={{ flex: 1, padding: "40px 48px", maxWidth: 860 }}>

        {/* ── OVERVIEW ── */}
        {active === "Overview" && (
          <div>
            <SectionTitle>Project Overview</SectionTitle>
            <p style={{ color: colors.textDim, lineHeight: 1.8, marginBottom: 24, fontSize: 14 }}>
              A fully standalone Wear OS application for Samsung Galaxy Watch 6 LTE comprising a data-rich watch face, ambient display mode, interactive action panel, and bidirectional phone communication — replacing WatchMaker scripting functionality lost in the Wear OS 6 / WFF transition.
            </p>

            <SubTitle>Goals</SubTitle>
            <Card accent={colors.accent}>
              <Row label="Watch operates standalone" value="Full LTE — no phone required for data" valueColor={colors.green} />
              <Row label="Data refresh" value="Preset intervals + manual triggers" />
              <Row label="Phone automation" value="Watch actions trigger phone tasks" valueColor={colors.amber} />
              <Row label="Phone → watch" value="Config push + notifications via DataClient / FCM" />
              <Row label="Distribution" value="Sideload APK (not Play Store)" />
            </Card>

            <SubTitle>Surfaces</SubTitle>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
              {[
                { name: "Main Watch Face", desc: "WFF XML — data-rich active display", color: colors.accent },
                { name: "Ambient / AOD Face", desc: "WFF XML — simple digital/analog, low power", color: colors.purple },
                { name: "Action Panel App", desc: "Jetpack Compose — multi-panel button grid", color: colors.amber },
                { name: "Phone Companion", desc: "Android app — config + action executor", color: colors.green },
              ].map(s => (
                <div key={s.name} style={{
                  background: colors.surfaceHigh,
                  border: `1px solid ${s.color}33`,
                  borderRadius: 8,
                  padding: 16,
                }}>
                  <div style={{ color: s.color, fontWeight: 700, fontSize: 13, marginBottom: 6, fontFamily: "monospace" }}>{s.name}</div>
                  <div style={{ color: colors.textDim, fontSize: 12 }}>{s.desc}</div>
                </div>
              ))}
            </div>

            <SubTitle>Constraints</SubTitle>
            <Card>
              <Row label="Watch Face Format" value="WFF required for Play Store; sideload allows AndroidX but WFF chosen for longevity" />
              <Row label="WFF limitation" value="No executable code in watch face — data fed via ComplicationDataSource" />
              <Row label="Background limits" value="Wear OS 6 enforces strict background execution — WorkManager required" />
              <Row label="LTE battery" value="Batch all API calls per refresh cycle; prefer WiFi/BT when available" />
            </Card>
          </div>
        )}

        {/* ── ARCHITECTURE ── */}
        {active === "Architecture" && (
          <div>
            <SectionTitle>System Architecture</SectionTitle>

            <SubTitle>Component Diagram</SubTitle>
            <CodeBlock>{`┌─────────────────────────────────────────────────┐
│              GALAXY WATCH 6 (LTE)               │
│                                                 │
│  ┌──────────────┐    ┌────────────────────┐     │
│  │  WFF Watch   │    │  Kotlin Service    │     │
│  │  Face (XML)  │◄───│  (Background)      │     │
│  │              │    │                    │     │
│  │  Reads compl.│    │  • WorkManager     │     │
│  │  data slots  │    │  • WeatherFetcher  │     │
│  │              │    │  • GPSProvider     │     │
│  │  Tap center  │    │  • SunCalc         │     │
│  │  ──────────► │    │  • ComplicationSrc │     │
│  └──────────────┘    └─────────┬──────────┘     │
│                                │                │
│  ┌──────────────────────────┐  │  LTE / WiFi    │
│  │  ActionPanelActivity     │  │                │
│  │  (Jetpack Compose)       │  ▼                │
│  │                          │  ┌─────────────┐  │
│  │  Panel 1 | Panel 2 | ... │  │ Open-Meteo  │  │
│  │  [btn][btn][btn][btn]    │  │ Weather API │  │
│  │  [btn][btn][btn][btn]    │  └─────────────┘  │
│  │                          │                   │
│  │  Tap btn ──► MessageClient ──────────────────┼──►
│  └──────────────────────────┘                   │
└─────────────────────────────────────────────────┘
                    │ Wearable Data Layer (BT/WiFi)
                    │ FCM (LTE push)
                    ▼
┌─────────────────────────────────────────────────┐
│               ANDROID PHONE                     │
│                                                 │
│  ┌────────────────┐   ┌──────────────────────┐  │
│  │ ConfigActivity │   │  WearableListener    │  │
│  │                │   │  Service             │  │
│  │ • Refresh rates│   │                      │  │
│  │ • Button map   │   │  onMessageReceived() │  │
│  │ • Units (°F/C) │   │  → executes actions  │  │
│  │                │   │  → sends confirm back│  │
│  └────────┬───────┘   └──────────────────────┘  │
│           │ DataClient.putDataItem()             │
│           └──────────────────────────────────►  │
└─────────────────────────────────────────────────┘`}</CodeBlock>

            <SubTitle>Data Flow — Watch Self-Refresh</SubTitle>
            <Card accent={colors.green}>
              <div style={{ fontSize: 13, color: colors.text, lineHeight: 2, fontFamily: "monospace" }}>
                WorkManager trigger<br/>
                → FusedLocationClient → coordinates<br/>
                → Geocoder → location name (MASON)<br/>
                → SensorManager → barometric altitude (MSL)<br/>
                → Open-Meteo API → temp, wind, conditions<br/>
                → SunriseSunsetCalc → sunrise / sunset times<br/>
                → ComplicationDataSource.update() → WFF slots<br/>
                → Watch face re-renders automatically
              </div>
            </Card>

            <SubTitle>Data Flow — Watch → Phone Action</SubTitle>
            <Card accent={colors.amber}>
              <div style={{ fontSize: 13, color: colors.text, lineHeight: 2, fontFamily: "monospace" }}>
                User taps action button on watch panel<br/>
                → MessageClient.sendMessage(nodeId, "/action", payload)<br/>
                → Phone WearableListenerService.onMessageReceived()<br/>
                → Execute phone action (camera / app launch / etc.)<br/>
                → MessageClient.sendMessage(watchNodeId, "/confirm", result)<br/>
                → Watch shows confirmation feedback
              </div>
            </Card>

            <SubTitle>Data Flow — Phone → Watch Config</SubTitle>
            <Card accent={colors.purple}>
              <div style={{ fontSize: 13, color: colors.text, lineHeight: 2, fontFamily: "monospace" }}>
                User changes setting in phone ConfigActivity<br/>
                → DataClient.putDataItem("/config", map)<br/>
                → Auto-synced when watch connects (BT/WiFi/LTE)<br/>
                → Watch DataListener.onDataChanged()<br/>
                → WorkManager constraints updated
              </div>
            </Card>
          </div>
        )}

        {/* ── WATCH FACE ── */}
        {active === "Watch Face" && (
          <div>
            <SectionTitle>Main Watch Face</SectionTitle>
            <p style={{ color: colors.textDim, fontSize: 13, lineHeight: 1.8, marginBottom: 20 }}>
              Built with Watch Face Format (WFF XML). All data elements are fed via <code style={{ color: colors.accent }}>ComplicationDataSource</code> slots from the background Kotlin service. The face is divided into distinct zones.
            </p>

            {/* Watch face diagram */}
            <SubTitle>Zone Layout</SubTitle>
            <div style={{ position: "relative", width: 260, height: 260, margin: "0 auto 32px", background: "#000", borderRadius: "50%", border: `2px solid ${colors.border}` }}>
              <WatchFaceZone label="WEATHER ROW" note="icon · temp · wind · condition" x={8} y={6} w={84} h={22} color={colors.amber} />
              <WatchFaceZone label="TIME HH:MM:SS" x={8} y={30} w={84} h={22} color={colors.accent} />
              <WatchFaceZone label="DATE" x={8} y={53} w={42} h={16} color={colors.purple} />
              <WatchFaceZone label="ALT CLOCK" x={53} y={53} w={38} h={16} color={colors.purple} />
              <WatchFaceZone label="SUNRISE" x={8} y={71} w={26} h={22} color={colors.amber} />
              <WatchFaceZone label="LOC + ALT" note="MASON · 1080 MSL" x={36} y={71} w={28} h={22} color={colors.green} />
              <WatchFaceZone label="SUNSET" x={66} y={71} w={26} h={22} color={colors.amber} />
              <div style={{ position: "absolute", left: "50%", top: "50%", transform: "translate(-50%, -50%)", width: 24, height: 24, borderRadius: "50%", border: `2px dashed ${colors.accent}88`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                <span style={{ fontSize: 7, color: colors.accent, fontFamily: "monospace" }}>TAP</span>
              </div>
            </div>

            <SubTitle>Complication Slots</SubTitle>
            {[
              { slot: "SLOT_WEATHER_ICON", type: "SMALL_IMAGE", source: "WeatherComplication", data: "Current conditions icon" },
              { slot: "SLOT_TEMPERATURE", type: "SHORT_TEXT", source: "WeatherComplication", data: "147°F / 63°C" },
              { slot: "SLOT_WIND", type: "SHORT_TEXT", source: "WeatherComplication", data: "16.1 mph + direction" },
              { slot: "SLOT_CONDITIONS", type: "SHORT_TEXT", source: "WeatherComplication", data: "CLOUDS / RAIN / etc." },
              { slot: "SLOT_NEXT_EVENT", type: "SHORT_TEXT", source: "CalendarComplication", data: "Next alarm or calendar event time" },
              { slot: "SLOT_DATE", type: "SHORT_TEXT", source: "SystemDataSource", data: "27TH FEB" },
              { slot: "SLOT_ALT_TIME", type: "SHORT_TEXT", source: "TimeZoneComplication", data: "Secondary timezone HH:MM" },
              { slot: "SLOT_SUNRISE", type: "SHORT_TEXT", source: "SunCalcComplication", data: "07:13" },
              { slot: "SLOT_SUNSET", type: "SHORT_TEXT", source: "SunCalcComplication", data: "18:28" },
              { slot: "SLOT_LOCATION", type: "SHORT_TEXT", source: "LocationComplication", data: "MASON" },
              { slot: "SLOT_ALTITUDE", type: "SHORT_TEXT", source: "LocationComplication", data: "1080 MSL" },
            ].map(c => (
              <div key={c.slot} style={{ display: "flex", alignItems: "flex-start", gap: 12, padding: "8px 0", borderBottom: `1px solid ${colors.border}` }}>
                <code style={{ color: colors.accent, fontSize: 11, minWidth: 180, fontFamily: "monospace" }}>{c.slot}</code>
                <Badge color={colors.purple}>{c.type}</Badge>
                <div style={{ flex: 1 }}>
                  <div style={{ color: colors.textDim, fontSize: 11, fontFamily: "monospace" }}>{c.source}</div>
                  <div style={{ color: colors.text, fontSize: 12 }}>{c.data}</div>
                </div>
              </div>
            ))}

            <SubTitle>Center Tap Action</SubTitle>
            <Card accent={colors.accent}>
              <div style={{ fontSize: 13, color: colors.textDim, lineHeight: 1.8 }}>
                The center of the watch face is defined as a tap target in WFF using <code style={{ color: colors.accent }}>&lt;TapAction&gt;</code>. Tapping launches <code style={{ color: colors.accent }}>ActionPanelActivity</code>. The entire visible circle is tappable; the action panel opens as a full-screen Wear OS activity.
              </div>
              <CodeBlock>{`<TapAction>
  <LaunchActivity
    activity="com.watchapp.ActionPanelActivity"
    package="com.watchapp" />
</TapAction>`}</CodeBlock>
            </Card>
          </div>
        )}

        {/* ── AMBIENT MODE ── */}
        {active === "Ambient Mode" && (
          <div>
            <SectionTitle>Ambient / Dim Face</SectionTitle>
            <p style={{ color: colors.textDim, fontSize: 13, lineHeight: 1.8, marginBottom: 20 }}>
              Defined in the same WFF XML file as the active face using the <code style={{ color: colors.accent }}>ambient="true"</code> scene attribute. Automatically switches when the watch enters ambient mode. Designed for minimum burn-in risk and battery use.
            </p>

            <SubTitle>Design Principles</SubTitle>
            <Card>
              <Row label="Colors" value="White on black only — no color in ambient" />
              <Row label="Pixel lit %" value="Target &lt;15% of screen pixels lit" />
              <Row label="Updates" value="Once per minute (no seconds in ambient)" />
              <Row label="No images" value="Icon slots hidden in ambient scene" />
              <Row label="No animations" value="Static display only" />
            </Card>

            <SubTitle>Option A — Minimal Digital</SubTitle>
            <div style={{ background: "#000", border: `1px solid ${colors.border}`, borderRadius: 12, padding: 24, textAlign: "center", marginBottom: 20 }}>
              <div style={{ fontSize: 36, fontFamily: "monospace", color: "#fff", fontWeight: 300, letterSpacing: "0.1em" }}>10:09</div>
              <div style={{ fontSize: 13, fontFamily: "monospace", color: "#888", marginTop: 6 }}>27 FEB</div>
              <div style={{ fontSize: 11, fontFamily: "monospace", color: "#555", marginTop: 4 }}>MASON</div>
            </div>

            <SubTitle>Option B — Simple Analog</SubTitle>
            <div style={{ background: "#000", border: `1px solid ${colors.border}`, borderRadius: 12, padding: 24, display: "flex", justifyContent: "center", marginBottom: 20 }}>
              <svg width={100} height={100} viewBox="0 0 100 100">
                <circle cx={50} cy={50} r={46} stroke="#222" strokeWidth={1} fill="none"/>
                {[0,30,60,90,120,150,180,210,240,270,300,330].map((deg, i) => {
                  const r = deg * Math.PI / 180;
                  const major = i % 3 === 0;
                  return <line key={deg}
                    x1={50 + 38 * Math.sin(r)} y1={50 - 38 * Math.cos(r)}
                    x2={50 + (major ? 44 : 42) * Math.sin(r)} y2={50 - (major ? 44 : 42) * Math.cos(r)}
                    stroke={major ? "#fff" : "#555"} strokeWidth={major ? 1.5 : 0.8}/>;
                })}
                {/* hour hand ~10 o'clock */}
                <line x1={50} y1={50} x2={32} y2={26} stroke="#fff" strokeWidth={2.5} strokeLinecap="round"/>
                {/* minute hand ~9 min */}
                <line x1={50} y1={50} x2={50} y2={18} stroke="#fff" strokeWidth={1.5} strokeLinecap="round"/>
                <circle cx={50} cy={50} r={2} fill="#fff"/>
                <text x={50} y={68} textAnchor="middle" fill="#555" fontSize={7} fontFamily="monospace">27 FEB</text>
              </svg>
            </div>

            <SubTitle>Recommendation</SubTitle>
            <Card accent={colors.green}>
              <div style={{ fontSize: 13, color: colors.text, lineHeight: 1.8 }}>
                <strong style={{ color: colors.green }}>Option A (Minimal Digital)</strong> is recommended. It is simpler to implement in WFF, has lower pixel density, and is consistent with the data-display theme of the active face. The analog option requires additional WFF arc/rotation elements and offers no practical advantage for a data-focused watch.
              </div>
            </Card>

            <SubTitle>WFF Scene Structure</SubTitle>
            <CodeBlock>{`<WatchFace>
  <!-- Active mode scene -->
  <Scene>
    <!-- All weather, time, location zones -->
    <!-- Seconds hand / display active -->
    <!-- Color icons visible -->
  </Scene>

  <!-- Ambient mode scene -->
  <Scene ambient="true">
    <!-- Time HH:MM only — no seconds -->
    <!-- Date line -->
    <!-- Location name only — no altitude -->
    <!-- All color/icon elements hidden -->
    <!-- White text on black only -->
  </Scene>
</WatchFace>`}</CodeBlock>
          </div>
        )}

        {/* ── ACTION PANELS ── */}
        {active === "Action Panels" && (
          <div>
            <SectionTitle>Action Panel App</SectionTitle>
            <p style={{ color: colors.textDim, fontSize: 13, lineHeight: 1.8, marginBottom: 20 }}>
              A full Jetpack Compose <code style={{ color: colors.accent }}>Activity</code> launched from the watch face center tap. Multiple panels navigated by horizontal swipe. Each button sends a <code style={{ color: colors.accent }}>MessageClient</code> message to the paired phone. Swipe-to-dismiss returns to watch face.
            </p>

            <SubTitle>Panel Navigation</SubTitle>
            <Card>
              <Row label="Navigation" value="HorizontalPager (Compose) — swipe left/right" />
              <Row label="Panel indicator" value="Dot row at bottom showing current panel" />
              <Row label="Dismiss" value="Swipe up or press hardware back → returns to watch face" />
              <Row label="Panel count" value="Configurable — default 3 panels" />
            </Card>

            <SubTitle>Panel Layout — Button Grid</SubTitle>
            <div style={{ background: "#000", border: `1px solid ${colors.border}`, borderRadius: "50%", width: 200, height: 200, margin: "16px auto", position: "relative", overflow: "hidden" }}>
              {[
                { x: 68, y: 10, label: "📷", sub: "PHOTO" },
                { x: 110, y: 10, label: "🎬", sub: "VIDEO" },
                { x: 28, y: 50, label: "10X", sub: "ZOOM" },
                { x: 68, y: 50, label: "3X", sub: "ZOOM" },
                { x: 110, y: 50, label: "1X", sub: "ZOOM" },
                { x: 150, y: 50, label: "0.6", sub: "ZOOM" },
                { x: 68, y: 120, label: "🎤", sub: "MIC" },
                { x: 110, y: 120, label: "🤳", sub: "SELFIE" },
              ].map((b, i) => (
                <div key={i} style={{
                  position: "absolute", left: b.x, top: b.y,
                  width: 36, height: 36, borderRadius: "50%",
                  background: "#1a1a2e",
                  border: `1px solid ${colors.border}`,
                  display: "flex", flexDirection: "column",
                  alignItems: "center", justifyContent: "center",
                }}>
                  <span style={{ fontSize: 12 }}>{b.label}</span>
                  <span style={{ fontSize: 6, color: colors.textDim, fontFamily: "monospace" }}>{b.sub}</span>
                </div>
              ))}
              <div style={{ position: "absolute", bottom: 16, left: "50%", transform: "translateX(-50%)", display: "flex", gap: 5 }}>
                {[0,1,2].map(i => <div key={i} style={{ width: 5, height: 5, borderRadius: "50%", background: i === 0 ? colors.accent : colors.border }} />)}
              </div>
            </div>

            <SubTitle>Predefined Panel Types</SubTitle>
            {[
              {
                panel: "Panel 1 — Camera Controls",
                color: colors.amber,
                buttons: ["Take Photo", "Start Video", "0.6x Zoom", "1x Zoom", "3x Zoom", "10x Zoom", "Selfie Mode", "Voice Trigger"],
              },
              {
                panel: "Panel 2 — Contacts / Quick Dial",
                color: colors.green,
                buttons: ["Contact 1 (photo)", "Contact 2 (photo)", "Contact 3 (photo)", "Contact 4 (photo)", "Recent Call 1", "Recent Call 2", "SMS Contact 1", "SMS Contact 2"],
              },
              {
                panel: "Panel 3 — App Shortcuts",
                color: colors.purple,
                buttons: ["TuneIn Radio", "Lock Screen", "Alarm", "Custom Action 1", "Custom Action 2", "Custom Action 3", "Custom Action 4", "Custom Action 5"],
              },
            ].map(p => (
              <Card key={p.panel} accent={p.color}>
                <div style={{ fontWeight: 700, color: p.color, fontSize: 13, fontFamily: "monospace", marginBottom: 10 }}>{p.panel}</div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                  {p.buttons.map(b => <Tag key={b}>{b}</Tag>)}
                </div>
              </Card>
            ))}

            <SubTitle>Button Message Payload</SubTitle>
            <CodeBlock>{`// Watch sends:
MessageClient.sendMessage(
    phoneNodeId,
    "/action",
    """{"type":"camera","action":"photo","extra":{}}""".toByteArray()
)

// Phone receives and routes:
when (payload.type) {
    "camera"  -> CameraController.execute(payload.action)
    "contact" -> PhoneDialer.call(payload.extra.contactId)
    "app"     -> AppLauncher.launch(payload.extra.packageName)
    "media"   -> MediaController.execute(payload.action)
}`}</CodeBlock>

            <SubTitle>Button Configuration</SubTitle>
            <Card>
              <div style={{ fontSize: 13, color: colors.textDim, lineHeight: 1.8 }}>
                Button assignments are stored as a <code style={{ color: colors.accent }}>DataClient</code> map pushed from the phone config app. Each button slot stores: <code style={{ color: colors.green }}>icon</code>, <code style={{ color: colors.green }}>label</code>, <code style={{ color: colors.green }}>actionType</code>, <code style={{ color: colors.green }}>actionPayload</code>. The watch reads this map on startup and on <code style={{ color: colors.accent }}>onDataChanged()</code>.
              </div>
            </Card>
          </div>
        )}

        {/* ── DATA & NETWORKING ── */}
        {active === "Data & Networking" && (
          <div>
            <SectionTitle>Data & Networking</SectionTitle>

            <SubTitle>Refresh Schedule</SubTitle>
            <Card>
              <Row label="Weather" value="Every 30 minutes (WorkManager periodic)" />
              <Row label="Location" value="On significant movement (&gt;500m) or every 60 min" />
              <Row label="Sunrise/sunset" value="Recalculated once daily at midnight" />
              <Row label="Alt. timezone" value="Static — set in config" />
              <Row label="Manual refresh" value="Long-press watch face or dedicated tile button" />
              <Row label="Network preference" value="WiFi first → BT tether → LTE (in that order)" />
            </Card>

            <SubTitle>Weather API — Open-Meteo</SubTitle>
            <Card accent={colors.green}>
              <Row label="Provider" value="Open-Meteo (free, no API key)" valueColor={colors.green} />
              <Row label="Endpoint" value="api.open-meteo.com/v1/forecast" />
              <Row label="Single call returns" value="Current temp, wind speed, wind direction, weather code" />
              <Row label="Units" value="Configurable °F/°C, mph/kph via phone config" />
            </Card>
            <CodeBlock>{`https://api.open-meteo.com/v1/forecast
  ?latitude={lat}
  &longitude={lon}
  &current=temperature_2m,wind_speed_10m,
           wind_direction_10m,weather_code
  &temperature_unit=fahrenheit
  &wind_speed_unit=mph
  &timezone=auto`}</CodeBlock>

            <SubTitle>Location — GPS + Reverse Geocoding</SubTitle>
            <Card>
              <Row label="Provider" value="FusedLocationProviderClient" />
              <Row label="Location name" value="Geocoder.getFromLocation() → locality (city name)" />
              <Row label="Altitude" value="SensorManager SENSOR_TYPE_PRESSURE → barometric MSL" />
              <Row label="Fallback altitude" value="GPS altitude if barometric unavailable" />
              <Row label="Permission" value="ACCESS_FINE_LOCATION in manifest" />
            </Card>

            <SubTitle>Sunrise / Sunset</SubTitle>
            <Card>
              <Row label="Library" value="commons-suncalc (Shredzone)" />
              <Row label="Input" value="Current GPS coordinates + date" />
              <Row label="Output" value="Sunrise time, sunset time (local timezone)" />
              <Row label="Recalc trigger" value="Daily at midnight OR on significant location change" />
            </Card>
            <CodeBlock>{`// Gradle dependency
implementation("org.shredzone.commons:commons-suncalc:3.9")

// Usage
val times = SunTimes.compute()
    .on(LocalDate.now())
    .at(latitude, longitude)
    .execute()

val sunrise = times.rise   // ZonedDateTime
val sunset  = times.set    // ZonedDateTime`}</CodeBlock>

            <SubTitle>WorkManager Constraints</SubTitle>
            <CodeBlock>{`val weatherRequest = PeriodicWorkRequestBuilder<WeatherWorker>(
    30, TimeUnit.MINUTES
).setConstraints(
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
).build()

// Worker respects Doze mode automatically
// Defers to WiFi/BT when available via NetworkType.UNMETERED
// Falls back to LTE when needed`}</CodeBlock>
          </div>
        )}

        {/* ── PHONE COMMUNICATION ── */}
        {active === "Phone Communication" && (
          <div>
            <SectionTitle>Phone Communication</SectionTitle>

            <SubTitle>API Selection Guide</SubTitle>
            <Card>
              <Row label="MessageClient" value="One-off commands, watch→phone actions, confirmations" valueColor={colors.amber} />
              <Row label="DataClient" value="Persistent config sync, button maps, settings" valueColor={colors.accent} />
              <Row label="FCM" value="Phone→watch push when not on BT (LTE/WiFi only)" valueColor={colors.green} />
              <Row label="ChannelClient" value="Not needed — no streaming data required" valueColor={colors.textMuted} />
            </Card>

            <SubTitle>Message Paths (Watch → Phone)</SubTitle>
            <ApiItem method="SEND" path="/action" desc="Trigger a phone action from watch button tap" dir="→" />
            <ApiItem method="SEND" path="/ping" desc="Connectivity check — watch tests phone is reachable" dir="→" />
            <ApiItem method="SEND" path="/config-request" desc="Watch requests fresh config from phone" dir="→" />

            <SubTitle>Message Paths (Phone → Watch)</SubTitle>
            <ApiItem method="SEND" path="/confirm" desc="Acknowledge action executed successfully" dir="←" />
            <ApiItem method="SEND" path="/error" desc="Report action failure with reason" dir="←" />
            <ApiItem method="SEND" path="/notify" desc="Push a notification text to watch display" dir="←" />

            <SubTitle>DataClient Keys (Phone → Watch sync)</SubTitle>
            <ApiItem method="DATA" path="/config/refresh-intervals" desc="Weather, location, and sun refresh rates (JSON)" dir="sync" />
            <ApiItem method="DATA" path="/config/units" desc="Temperature unit (F/C), speed unit (mph/kph)" dir="sync" />
            <ApiItem method="DATA" path="/config/timezone-secondary" desc="Second clock timezone string" dir="sync" />
            <ApiItem method="DATA" path="/config/buttons" desc="Panel button assignments map (JSON array)" dir="sync" />

            <SubTitle>WearableListenerService — Both Sides</SubTitle>
            <CodeBlock>{`// Registered in AndroidManifest.xml on BOTH watch and phone APKs
<service android:name=".WatchMessageService"
    android:exported="true">
  <intent-filter>
    <action android:name=
      "com.google.android.gms.wearable.MESSAGE_RECEIVED"/>
    <data android:scheme="wear"
          android:host="*"
          android:pathPrefix="/action"/>
  </intent-filter>
</service>

// Works even when app is closed — OS delivers messages
// to the service directly`}</CodeBlock>

            <SubTitle>FCM — Phone Pushes to Watch</SubTitle>
            <Card accent={colors.green}>
              <div style={{ fontSize: 13, color: colors.textDim, lineHeight: 1.8 }}>
                For phone-initiated pushes when the watch is on LTE only (no BT), FCM is the recommended channel. The watch registers an FCM token on first run and sends it to the phone via DataClient. The phone uses the token to push messages via FCM REST endpoint. Works with Doze mode.
              </div>
            </Card>

            <SubTitle>Node Discovery</SubTitle>
            <CodeBlock>{`// Find connected phone node before sending message
Wearable.getNodeClient(context)
    .connectedNodes
    .addOnSuccessListener { nodes ->
        nodes.firstOrNull { it.isNearby }
            ?.let { node ->
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/action", payload)
            }
    }`}</CodeBlock>
          </div>
        )}

        {/* ── PROJECT STRUCTURE ── */}
        {active === "Project Structure" && (
          <div>
            <SectionTitle>Project Structure</SectionTitle>
            <p style={{ color: colors.textDim, fontSize: 13, marginBottom: 20, lineHeight: 1.8 }}>
              Single Android Studio project with three modules sharing common data models via a <code style={{ color: colors.accent }}>:shared</code> Kotlin module.
            </p>

            <CodeBlock>{`:watchapp/                     ← Root project
│
├── :shared/                   ← Kotlin Multiplatform shared code
│   ├── ActionMessage.kt        ← Data classes for MessageClient payloads
│   ├── ConfigModel.kt          ← Config schema (units, intervals, buttons)
│   └── ButtonConfig.kt         ← Button slot data model
│
├── :wear/                     ← Wear OS module
│   ├── AndroidManifest.xml
│   ├── watchface/
│   │   └── watchface.xml       ← WFF XML (active + ambient scenes)
│   ├── complications/
│   │   ├── WeatherComplication.kt
│   │   ├── LocationComplication.kt
│   │   ├── SunCalcComplication.kt
│   │   └── TimeZoneComplication.kt
│   ├── workers/
│   │   ├── WeatherWorker.kt    ← WorkManager: fetches weather API
│   │   ├── LocationWorker.kt   ← WorkManager: updates GPS + geocode
│   │   └── SunCalcWorker.kt    ← WorkManager: sunrise/sunset
│   ├── ui/
│   │   ├── ActionPanelActivity.kt   ← Compose multi-panel grid
│   │   ├── PanelScreen.kt           ← Single panel Composable
│   │   └── ActionButton.kt          ← Circular button Composable
│   └── comms/
│       ├── WatchMessageService.kt   ← WearableListenerService
│       └── ConfigListener.kt        ← DataClient onChange handler
│
└── :phone/                    ← Android phone module
    ├── AndroidManifest.xml
    ├── ui/
    │   ├── ConfigActivity.kt   ← Settings: intervals, units, buttons
    │   └── ButtonMapEditor.kt  ← Drag-to-assign button config UI
    ├── actions/
    │   ├── ActionRouter.kt     ← Routes incoming /action messages
    │   ├── CameraController.kt
    │   ├── AppLauncher.kt
    │   └── ContactDialer.kt
    └── comms/
        ├── PhoneMessageService.kt   ← WearableListenerService
        └── ConfigPusher.kt          ← DataClient config sync`}</CodeBlock>

            <SubTitle>Gradle Dependencies — :wear module</SubTitle>
            <CodeBlock>{`// Wear OS core
implementation("androidx.wear:wear:1.3.0")
implementation("androidx.wear.compose:compose-material3:1.5.0")
implementation("androidx.wear.compose:compose-foundation:1.5.0")

// Watch Face
implementation("androidx.wear.watchface:watchface:1.2.1")
implementation("androidx.wear.watchface:watchface-complications-data-source:1.2.1")

// Wearable communication
implementation("com.google.android.gms:play-services-wearable:18.2.0")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.1")

// Networking
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

// Location
implementation("com.google.android.gms:play-services-location:21.3.0")

// Sun calculation
implementation("org.shredzone.commons:commons-suncalc:3.9")`}</CodeBlock>
          </div>
        )}

        {/* ── TECH STACK ── */}
        {active === "Tech Stack" && (
          <div>
            <SectionTitle>Tech Stack</SectionTitle>

            <SubTitle>Languages & Frameworks</SubTitle>
            <Card>
              <Row label="Language" value="Kotlin" valueColor={colors.purple} />
              <Row label="Watch face format" value="WFF XML (Watch Face Format v4)" valueColor={colors.accent} />
              <Row label="Watch UI" value="Jetpack Compose for Wear OS (Material 3 Expressive)" valueColor={colors.accent} />
              <Row label="Phone UI" value="Jetpack Compose (Android)" valueColor={colors.accent} />
              <Row label="Build system" value="Gradle (Kotlin DSL)" />
              <Row label="Min SDK" value="API 30 (Android 11 / Wear OS 3)" />
              <Row label="Target SDK" value="API 35 (Android 15 / Wear OS 6)" />
            </Card>

            <SubTitle>Key Libraries</SubTitle>
            {[
              { name: "androidx.wear.watchface", purpose: "Watch face + complication data source API", tag: ":wear" },
              { name: "Wear Compose Material 3", purpose: "Round-display-optimised Compose components", tag: ":wear" },
              { name: "play-services-wearable", purpose: "DataClient, MessageClient, NodeClient", tag: "both" },
              { name: "WorkManager", purpose: "Scheduled background data refresh (battery-safe)", tag: ":wear" },
              { name: "OkHttp 4", purpose: "HTTP client for Open-Meteo weather API", tag: ":wear" },
              { name: "kotlinx.serialization", purpose: "JSON parsing for API responses + message payloads", tag: "both" },
              { name: "play-services-location", purpose: "FusedLocationProviderClient for GPS", tag: ":wear" },
              { name: "commons-suncalc", purpose: "Sunrise/sunset calculation from coordinates", tag: ":wear" },
              { name: "Firebase Cloud Messaging", purpose: "Phone → watch push over LTE", tag: "both" },
            ].map(l => (
              <div key={l.name} style={{ display: "flex", alignItems: "flex-start", gap: 12, padding: "8px 0", borderBottom: `1px solid ${colors.border}` }}>
                <code style={{ color: colors.accent, fontSize: 11, minWidth: 220, fontFamily: "monospace" }}>{l.name}</code>
                <div style={{ flex: 1, fontSize: 12, color: colors.text }}>{l.purpose}</div>
                <Badge color={l.tag === ":wear" ? colors.amber : l.tag === "both" ? colors.green : colors.purple}>{l.tag}</Badge>
              </div>
            ))}

            <SubTitle>Development Tools</SubTitle>
            <Card>
              <Row label="IDE" value="Android Studio Meerkat (2024.3) or newer" />
              <Row label="Emulator" value="Wear OS Large Round API 35 emulator" />
              <Row label="WFF Validator" value="Google open-source WFF validator (Gradle task)" />
              <Row label="Watch Face Studio" value="Optional — Samsung WFS for initial layout prototyping" />
              <Row label="ADB" value="Used for sideloading APK directly to watch" />
            </Card>

            <SubTitle>Sideloading (No Play Store)</SubTitle>
            <Card accent={colors.amber}>
              <div style={{ fontSize: 13, color: colors.textDim, lineHeight: 1.8, marginBottom: 10 }}>
                Since this app is personal use, no Play Store submission is required. Sideload directly via ADB over WiFi (no USB needed with Watch 6).
              </div>
              <CodeBlock>{`# Enable ADB over WiFi on watch:
# Settings → Developer Options → Wireless Debugging → ON

# Connect and install
adb connect <watch-ip>:5555
adb -s <watch-ip>:5555 install -r watchapp-wear-release.apk
adb -s <phone-id> install -r watchapp-phone-release.apk`}</CodeBlock>
            </Card>

            <SubTitle>Open Questions / Decisions Pending</SubTitle>
            {[
              { q: "Analog vs digital ambient face", status: "DECIDE", color: colors.amber },
              { q: "Number of action panels (default 3?)", status: "DECIDE", color: colors.amber },
              { q: "Default button assignments per panel", status: "DEFINE", color: colors.amber },
              { q: "Secondary timezone — fixed or user-selectable?", status: "DECIDE", color: colors.amber },
              { q: "Temperature units default (°F shown in screenshot)", status: "CONFIRM", color: colors.green },
              { q: "Altitude preference — barometric or GPS?", status: "DECIDE", color: colors.amber },
            ].map(item => (
              <div key={item.q} style={{ display: "flex", alignItems: "center", gap: 12, padding: "8px 0", borderBottom: `1px solid ${colors.border}` }}>
                <Badge color={item.color}>{item.status}</Badge>
                <span style={{ fontSize: 13, color: colors.text }}>{item.q}</span>
              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  );
}
