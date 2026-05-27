import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, AlertTriangle, Image, Timer, Moon, HelpCircle, Info, ChevronRight, FolderOpen, Database } from 'lucide-react'

export default function SettingsScreen() {
  const navigate = useNavigate()
  const [remainingDays] = useState(5)

  const getValidityStyle = () => {
    if (remainingDays <= 0) return { bg: '#fff2f0', color: '#f5222d' }
    if (remainingDays <= 3) return { bg: '#fffbe6', color: '#faad14' }
    return { bg: '#e6faf5', color: '#00d4aa' }
  }

  const validityStyle = getValidityStyle()

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: 'white' }}>
      {/* Header */}
      <div style={{
        height: 56,
        display: 'flex',
        alignItems: 'center',
        padding: '0 8px',
        background: 'white',
        borderBottom: '1px solid #f0f0f0'
      }}>
        <button 
          onClick={() => navigate(-1)}
          style={{ background: 'none', border: 'none', padding: 8, cursor: 'pointer' }}
        >
          <ArrowLeft size={24} color="#333" />
        </button>
        <span style={{ fontSize: 18, fontWeight: 500, marginLeft: 8 }}>设置</span>
      </div>

      {/* Content */}
      <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
        {/* Validity Card */}
        <div style={{
          background: validityStyle.bg,
          borderRadius: 12,
          padding: 16,
          marginBottom: 24
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <AlertTriangle size={20} color={validityStyle.color} />
            <span style={{ fontSize: 16, color: validityStyle.color, fontWeight: 500 }}>版本有效期</span>
          </div>
          <div style={{ fontSize: 32, fontWeight: 'bold', color: validityStyle.color, marginBottom: 16 }}>
            剩余 {remainingDays} 天
          </div>
          <button style={{
            width: '100%',
            padding: '12px',
            background: '#00d4aa',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            fontSize: 16,
            fontWeight: 500,
            cursor: 'pointer'
          }}>
            获取新版本
          </button>
        </div>

        {/* Preferences */}
        <SectionTitle>偏好设置</SectionTitle>
        <SettingsItem icon={<Image size={24} color="#666" />} title="默认导出格式" value="JPEG" />
        <SettingsItem icon={<Database size={24} color="#666" />} title="默认导出质量" value="90%" />
        <SettingsItem icon={<Timer size={24} color="#666" />} title="自动保存间隔" value="30秒" />
        <SettingsItem icon={<Moon size={24} color="#666" />} title="界面主题" value="跟随系统" />

        {/* Storage */}
        <SectionTitle>存储管理</SectionTitle>
        <SettingsItem icon={<FolderOpen size={24} color="#666" />} title="缓存清理" value="" />
        <SettingsItem icon={<Database size={24} color="#666" />} title="草稿管理" value="" />
        <SettingsItem icon={<Database size={24} color="#666" />} title="存储空间统计" value="" />

        {/* About */}
        <SectionTitle>关于</SectionTitle>
        <SettingsItem icon={<Info size={24} color="#666" />} title="版本号" value="v1.0.0" showArrow={false} />
        <SettingsItem icon={<HelpCircle size={24} color="#666" />} title="使用帮助" value="" />
      </div>
    </div>
  )
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ fontSize: 14, color: '#999', fontWeight: 500, marginBottom: 8, marginTop: 24 }}>
      {children}
    </div>
  )
}

function SettingsItem({ 
  icon, 
  title, 
  value, 
  showArrow = true 
}: { 
  icon: React.ReactNode
  title: string
  value: string
  showArrow?: boolean
}) {
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      padding: '12px 0',
      borderBottom: '1px solid #f5f5f5',
      cursor: 'pointer'
    }}>
      {icon}
      <span style={{ fontSize: 16, marginLeft: 16, flex: 1 }}>{title}</span>
      {value && <span style={{ fontSize: 14, color: '#999', marginRight: 8 }}>{value}</span>}
      {showArrow && <ChevronRight size={20} color="#ccc" />}
    </div>
  )
}
