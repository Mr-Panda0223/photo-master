import { useState } from 'react'
import { 
  User, 
  Palette, 
  Globe, 
  Shield, 
  History, 
  Clock, 
  ChevronRight,
  LogIn,
  Moon,
  Sun,
  Smartphone,
  Image as ImageIcon,
  Camera,
  FolderOpen
} from 'lucide-react'

interface MenuItemProps {
  icon: React.ReactNode
  title: string
  subtitle?: string
  onClick?: () => void
  rightContent?: React.ReactNode
}

const MenuItem = ({ icon, title, subtitle, onClick, rightContent }: MenuItemProps) => (
  <div 
    onClick={onClick}
    style={{
      display: 'flex',
      alignItems: 'center',
      padding: '16px 20px',
      background: 'white',
      borderBottom: '1px solid #f0f0f0',
      cursor: onClick ? 'pointer' : 'default'
    }}
  >
    <div style={{ 
      width: 36, 
      height: 36, 
      borderRadius: 10, 
      background: '#f5f5f5',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      marginRight: 12,
      color: '#333'
    }}>
      {icon}
    </div>
    <div style={{ flex: 1 }}>
      <div style={{ fontSize: 15, fontWeight: 500, color: '#333' }}>{title}</div>
      {subtitle && (
        <div style={{ fontSize: 12, color: '#999', marginTop: 2 }}>{subtitle}</div>
      )}
    </div>
    {rightContent || (onClick && <ChevronRight size={18} color="#ccc" />)}
  </div>
)

const SectionTitle = ({ title }: { title: string }) => (
  <div style={{ 
    padding: '20px 20px 8px', 
    fontSize: 13, 
    fontWeight: 600, 
    color: '#00d4aa',
    textTransform: 'uppercase',
    letterSpacing: 0.5
  }}>
    {title}
  </div>
)

export default function ProfileScreen() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [darkMode, setDarkMode] = useState(false)
  const [language, setLanguage] = useState('zh-CN')
  
  // 计算剩余时间（模拟7天试用期）
  const getRemainingDays = () => {
    const installDate = localStorage.getItem('installDate')
    if (!installDate) {
      localStorage.setItem('installDate', new Date().toISOString())
      return 7
    }
    const install = new Date(installDate)
    const now = new Date()
    const diffTime = now.getTime() - install.getTime()
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
    const remaining = Math.max(0, 7 - diffDays)
    return remaining
  }
  
  const remainingDays = getRemainingDays()
  
  const languages = [
    { code: 'zh-CN', name: '简体中文' },
    { code: 'zh-TW', name: '繁體中文' },
    { code: 'en', name: 'English' },
    { code: 'ja', name: '日本語' }
  ]
  
  const [showLanguageModal, setShowLanguageModal] = useState(false)
  
  return (
    <div style={{ 
      minHeight: '100vh', 
      background: '#f5f5f5',
      paddingBottom: 80
    }}>
      {/* 头部区域 */}
      <div style={{
        background: 'linear-gradient(135deg, #00d4aa 0%, #00a884 100%)',
        padding: '40px 20px 30px',
        color: 'white'
      }}>
        <div style={{ fontSize: 24, fontWeight: 700, marginBottom: 8 }}>我的</div>
        <div style={{ fontSize: 14, opacity: 0.9 }}>管理您的账户和设置</div>
      </div>
      
      {/* 用户信息卡片 */}
      <div style={{ padding: '0 16px', marginTop: -20 }}>
        <div style={{
          background: 'white',
          borderRadius: 16,
          padding: 24,
          boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
          display: 'flex',
          alignItems: 'center',
          gap: 16
        }}>
          <div style={{
            width: 64,
            height: 64,
            borderRadius: 32,
            background: isLoggedIn ? '#00d4aa' : '#e0e0e0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            {isLoggedIn ? (
              <User size={32} color="white" />
            ) : (
              <LogIn size={28} color="#999" />
            )}
          </div>
          <div style={{ flex: 1 }}>
            {isLoggedIn ? (
              <>
                <div style={{ fontSize: 18, fontWeight: 600, color: '#333' }}>用户_9527</div>
                <div style={{ fontSize: 13, color: '#999', marginTop: 4 }}>已登录</div>
              </>
            ) : (
              <>
                <div style={{ fontSize: 18, fontWeight: 600, color: '#333' }}>未登录</div>
                <div style={{ fontSize: 13, color: '#999', marginTop: 4 }}>登录后可同步编辑记录</div>
              </>
            )}
          </div>
          <button
            onClick={() => setIsLoggedIn(!isLoggedIn)}
            style={{
              padding: '8px 20px',
              borderRadius: 20,
              border: 'none',
              background: isLoggedIn ? '#ff6b6b' : '#00d4aa',
              color: 'white',
              fontSize: 14,
              fontWeight: 500,
              cursor: 'pointer'
            }}
          >
            {isLoggedIn ? '退出' : '登录'}
          </button>
        </div>
      </div>
      
      {/* 剩余使用时间 - 突出显示 */}
      <div style={{ padding: '0 16px', marginTop: 16 }}>
        <div style={{
          background: remainingDays <= 2 ? '#fff5f5' : '#f0fff4',
          borderRadius: 12,
          padding: 20,
          border: `1px solid ${remainingDays <= 2 ? '#feb2b2' : '#9ae6b4'}`,
          display: 'flex',
          alignItems: 'center',
          gap: 16
        }}>
          <div style={{
            width: 48,
            height: 48,
            borderRadius: 24,
            background: remainingDays <= 2 ? '#feb2b2' : '#9ae6b4',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Clock size={24} color={remainingDays <= 2 ? '#c53030' : '#22543d'} />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 14, color: remainingDays <= 2 ? '#c53030' : '#22543d', fontWeight: 500 }}>
              剩余使用时间
            </div>
            <div style={{ fontSize: 20, fontWeight: 700, color: remainingDays <= 2 ? '#c53030' : '#22543d', marginTop: 4 }}>
              {remainingDays} 天
            </div>
          </div>
          {remainingDays <= 2 && (
            <div style={{
              padding: '6px 12px',
              background: '#c53030',
              color: 'white',
              borderRadius: 12,
              fontSize: 12,
              fontWeight: 500
            }}>
              即将到期
            </div>
          )}
        </div>
      </div>
      
      {/* 设置区域 */}
      <SectionTitle title="应用设置" />
      <div style={{ margin: '0 16px', borderRadius: 12, overflow: 'hidden' }}>
        <MenuItem 
          icon={<Palette size={18} />}
          title="外观风格"
          subtitle={darkMode ? '深色模式' : '浅色模式'}
          rightContent={
            <button
              onClick={(e) => {
                e.stopPropagation()
                setDarkMode(!darkMode)
              }}
              style={{
                width: 50,
                height: 28,
                borderRadius: 14,
                border: 'none',
                background: darkMode ? '#00d4aa' : '#e0e0e0',
                position: 'relative',
                cursor: 'pointer',
                transition: 'all 0.3s'
              }}
            >
              <div style={{
                width: 24,
                height: 24,
                borderRadius: 12,
                background: 'white',
                position: 'absolute',
                top: 2,
                left: darkMode ? 24 : 2,
                transition: 'all 0.3s',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                {darkMode ? <Moon size={14} /> : <Sun size={14} />}
              </div>
            </button>
          }
        />
        <MenuItem 
          icon={<Globe size={18} />}
          title="语言设置"
          subtitle={languages.find(l => l.code === language)?.name}
          onClick={() => setShowLanguageModal(true)}
        />
      </div>
      
      {/* 权限设置 */}
      <SectionTitle title="权限管理" />
      <div style={{ margin: '0 16px', borderRadius: 12, overflow: 'hidden' }}>
        <MenuItem 
          icon={<ImageIcon size={18} />}
          title="相册访问权限"
          subtitle="已授权"
          rightContent={
            <div style={{
              padding: '4px 10px',
              background: '#e6fffa',
              color: '#00a884',
              borderRadius: 10,
              fontSize: 12,
              fontWeight: 500
            }}>
              已开启
            </div>
          }
        />
        <MenuItem 
          icon={<Camera size={18} />}
          title="相机权限"
          subtitle="未授权"
          rightContent={
            <div style={{
              padding: '4px 10px',
              background: '#fff5f5',
              color: '#c53030',
              borderRadius: 10,
              fontSize: 12,
              fontWeight: 500
            }}>
              去开启
            </div>
          }
        />
        <MenuItem 
          icon={<FolderOpen size={18} />}
          title="存储权限"
          subtitle="已授权"
          rightContent={
            <div style={{
              padding: '4px 10px',
              background: '#e6fffa',
              color: '#00a884',
              borderRadius: 10,
              fontSize: 12,
              fontWeight: 500
            }}>
              已开启
            </div>
          }
        />
      </div>
      
      {/* 编辑记录 */}
      <SectionTitle title="编辑记录" />
      <div style={{ margin: '0 16px', borderRadius: 12, overflow: 'hidden' }}>
        <MenuItem 
          icon={<History size={18} />}
          title="编辑历史"
          subtitle="查看所有编辑记录"
          onClick={() => {}}
        />
        <MenuItem 
          icon={<Smartphone size={18} />}
          title="设备信息"
          subtitle="Android 14 • v1.0.0"
        />
      </div>
      
      {/* 关于 */}
      <div style={{ 
        margin: '24px 16px',
        padding: 20,
        background: 'white',
        borderRadius: 12,
        textAlign: 'center'
      }}>
        <div style={{ fontSize: 14, color: '#999' }}>PhotoPS 专业版</div>
        <div style={{ fontSize: 12, color: '#ccc', marginTop: 4 }}>版本 1.0.0 (Build 20240115)</div>
      </div>
      
      {/* 语言选择弹窗 */}
      {showLanguageModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'center',
          zIndex: 1000
        }}
        onClick={() => setShowLanguageModal(false)}
        >
          <div 
            style={{
              background: 'white',
              width: '100%',
              maxWidth: 400,
              borderRadius: '20px 20px 0 0',
              padding: 20,
              animation: 'slideUp 0.3s ease'
            }}
            onClick={e => e.stopPropagation()}
          >
            <div style={{ 
              fontSize: 18, 
              fontWeight: 600, 
              textAlign: 'center',
              marginBottom: 20,
              color: '#333'
            }}>
              选择语言
            </div>
            {languages.map((lang) => (
              <div
                key={lang.code}
                onClick={() => {
                  setLanguage(lang.code)
                  setShowLanguageModal(false)
                }}
                style={{
                  padding: '16px 20px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  borderBottom: '1px solid #f0f0f0',
                  cursor: 'pointer'
                }}
              >
                <span style={{ fontSize: 15, color: '#333' }}>{lang.name}</span>
                {language === lang.code && (
                  <div style={{
                    width: 20,
                    height: 20,
                    borderRadius: 10,
                    background: '#00d4aa',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}>
                    <div style={{
                      width: 8,
                      height: 8,
                      borderRadius: 4,
                      background: 'white'
                    }} />
                  </div>
                )}
              </div>
            ))}
            <button
              onClick={() => setShowLanguageModal(false)}
              style={{
                width: '100%',
                padding: 16,
                marginTop: 20,
                borderRadius: 12,
                border: 'none',
                background: '#f5f5f5',
                color: '#666',
                fontSize: 15,
                fontWeight: 500,
                cursor: 'pointer'
              }}
            >
              取消
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
