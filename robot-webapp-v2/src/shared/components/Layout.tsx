import { NavLink, Outlet } from 'react-router-dom'
import { useWebSocketStore } from '../websocket/websocketStore'
import { useEffect } from 'react'
import styles from './Layout.module.css'

const NAV_ITEMS = [
  { to: '/live',       label: 'Live',          icon: '📷' },
  { to: '/animation',  label: 'Animation',     icon: '🎬' },
  { to: '/dialogue',   label: 'Dialogue',      icon: '💬' },
  { to: '/config',     label: 'Configuration', icon: '⚙️' },
  { to: '/monitoring', label: 'Monitoring',    icon: '📊' },
]

export function Layout() {
  const { connect, disconnect, connected } = useWebSocketStore()

  useEffect(() => {
    connect()
    return () => disconnect()
  }, [])

  return (
    <div className={styles.shell}>
      <nav className={styles.nav}>
        <span className={styles.logo}>🤖 Wall-E</span>
        <div className={styles.navLinks}>
          {NAV_ITEMS.map(({ to, label, icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) => `${styles.navLink} ${isActive ? styles.active : ''}`}
            >
              <span className={styles.navIcon}>{icon}</span>
              <span className={styles.navLabel}>{label}</span>
            </NavLink>
          ))}
        </div>
        <span className={styles.status}>
          <span className={connected ? styles.dotOn : styles.dotOff} />
          {connected ? 'Connecté' : 'Déconnecté'}
        </span>
      </nav>
      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  )
}
