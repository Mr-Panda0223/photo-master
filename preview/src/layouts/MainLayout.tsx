import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Home, User } from 'lucide-react'

export default function MainLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const currentPath = location.pathname

  const tabs = [
    { path: '/', name: '首页', icon: Home },
    { path: '/profile', name: '我的', icon: User },
  ]

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Content */}
      <div style={{ flex: 1, overflow: 'auto' }}>
        <Outlet />
      </div>

      {/* Bottom Tab Bar */}
      <div style={{
        height: 56,
        display: 'flex',
        background: 'white',
        borderTop: '1px solid #f0f0f0'
      }}>
        {tabs.map(tab => {
          const Icon = tab.icon
          const isActive = currentPath === tab.path
          return (
            <div
              key={tab.path}
              onClick={() => navigate(tab.path)}
              style={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 4,
                cursor: 'pointer',
                background: isActive ? '#e6faf5' : 'transparent'
              }}
            >
              <Icon 
                size={24} 
                color={isActive ? '#00d4aa' : '#999'} 
              />
              <span style={{ 
                fontSize: 12, 
                color: isActive ? '#00d4aa' : '#999',
                fontWeight: isActive ? 500 : 400
              }}>
                {tab.name}
              </span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
