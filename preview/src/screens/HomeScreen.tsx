import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Clock, CheckCircle, Image as ImageIcon } from 'lucide-react'

// Mock data
const mockDrafts = [
  { id: 'draft_1', name: '未命名项目 1', lastEdit: '10分钟前', thumbnail: '' },
  { id: 'draft_2', name: '未命名项目 2', lastEdit: '2小时前', thumbnail: '' },
]

const mockCompleted = [
  { id: 'completed_1', name: 'IMG_20240115.jpg', date: '2024-01-15', thumbnail: '' },
  { id: 'completed_2', name: 'IMG_20240114.jpg', date: '2024-01-14', thumbnail: '' },
  { id: 'completed_3', name: 'IMG_20240113.jpg', date: '2024-01-13', thumbnail: '' },
  { id: 'completed_4', name: 'IMG_20240112.jpg', date: '2024-01-12', thumbnail: '' },
]

export default function HomeScreen() {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<'drafts' | 'completed'>('drafts')

  const handleNewEdit = () => {
    // 打开系统相册选择图片
    navigate('/editor/new')
  }

  const handleDraftClick = (draftId: string) => {
    navigate(`/editor/${draftId}`)
  }

  const handleCompletedClick = (completedId: string) => {
    // 查看已完成的图片或重新编辑
    navigate(`/editor/${completedId}`)
  }

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: '#f5f5f5' }}>
      {/* Header */}
      <div style={{
        height: 56,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 16px',
        background: 'white',
        borderBottom: '1px solid #f0f0f0'
      }}>
        <span style={{ fontSize: 20, fontWeight: 600, color: '#00d4aa' }}>PhotoMaster</span>
        <button 
          onClick={handleNewEdit}
          style={{
            background: '#00d4aa',
            border: 'none',
            padding: '8px 16px',
            borderRadius: 20,
            color: 'white',
            fontSize: 14,
            fontWeight: 500,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: 4
          }}
        >
          <Plus size={18} />
          <span>新建</span>
        </button>
      </div>

      {/* Tab Switcher */}
      <div style={{
        display: 'flex',
        background: 'white',
        borderBottom: '1px solid #f0f0f0'
      }}>
        <TabButton 
          active={activeTab === 'drafts'} 
          onClick={() => setActiveTab('drafts')}
          icon={<Clock size={16} />}
          label="正在编辑"
          count={mockDrafts.length}
        />
        <TabButton 
          active={activeTab === 'completed'} 
          onClick={() => setActiveTab('completed')}
          icon={<CheckCircle size={16} />}
          label="已完成"
          count={mockCompleted.length}
        />
      </div>

      {/* Content */}
      <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
        {activeTab === 'drafts' ? (
          <DraftsList drafts={mockDrafts} onDraftClick={handleDraftClick} />
        ) : (
          <CompletedList completed={mockCompleted} onCompletedClick={handleCompletedClick} />
        )}
      </div>
    </div>
  )
}

function TabButton({ 
  active, 
  onClick, 
  icon, 
  label,
  count 
}: { 
  active: boolean
  onClick: () => void
  icon: React.ReactNode
  label: string
  count: number
}) {
  return (
    <button
      onClick={onClick}
      style={{
        flex: 1,
        padding: '12px',
        background: 'transparent',
        border: 'none',
        borderBottom: active ? '2px solid #00d4aa' : '2px solid transparent',
        color: active ? '#00d4aa' : '#666',
        fontSize: 14,
        fontWeight: active ? 500 : 400,
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6
      }}
    >
      {icon}
      <span>{label}</span>
      <span style={{
        background: active ? '#e6faf5' : '#f5f5f5',
        color: active ? '#00d4aa' : '#999',
        padding: '2px 8px',
        borderRadius: 10,
        fontSize: 12
      }}>
        {count}
      </span>
    </button>
  )
}

function DraftsList({ 
  drafts, 
  onDraftClick 
}: { 
  drafts: typeof mockDrafts
  onDraftClick: (id: string) => void 
}) {
  if (drafts.length === 0) {
    return (
      <EmptyState 
        icon={<Clock size={48} color="#ccc" />}
        title="没有正在编辑的项目"
        subtitle="点击右上角新建开始编辑照片"
      />
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {drafts.map(draft => (
        <div
          key={draft.id}
          onClick={() => onDraftClick(draft.id)}
          style={{
            background: 'white',
            borderRadius: 12,
            padding: 12,
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            cursor: 'pointer',
            boxShadow: '0 1px 3px rgba(0,0,0,0.08)'
          }}
        >
          <div style={{
            width: 60,
            height: 60,
            background: '#f5f5f5',
            borderRadius: 8,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <ImageIcon size={28} color="#ccc" />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 16, fontWeight: 500, color: '#333', marginBottom: 4 }}>
              {draft.name}
            </div>
            <div style={{ fontSize: 12, color: '#999' }}>
              最后编辑: {draft.lastEdit}
            </div>
          </div>
          <div style={{
            background: '#fffbe6',
            color: '#faad14',
            padding: '4px 8px',
            borderRadius: 4,
            fontSize: 12
          }}>
            草稿
          </div>
        </div>
      ))}
    </div>
  )
}

function CompletedList({ 
  completed, 
  onCompletedClick 
}: { 
  completed: typeof mockCompleted
  onCompletedClick: (id: string) => void 
}) {
  if (completed.length === 0) {
    return (
      <EmptyState 
        icon={<CheckCircle size={48} color="#ccc" />}
        title="没有已完成的项目"
        subtitle="完成编辑的照片会显示在这里"
      />
    )
  }

  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
      {completed.map(item => (
        <div
          key={item.id}
          onClick={() => onCompletedClick(item.id)}
          style={{
            background: 'white',
            borderRadius: 12,
            overflow: 'hidden',
            cursor: 'pointer',
            boxShadow: '0 1px 3px rgba(0,0,0,0.08)'
          }}
        >
          <div style={{
            aspectRatio: '1',
            background: '#f5f5f5',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <ImageIcon size={40} color="#ccc" />
          </div>
          <div style={{ padding: 12 }}>
            <div style={{ fontSize: 14, color: '#333', marginBottom: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {item.name}
            </div>
            <div style={{ fontSize: 12, color: '#999' }}>
              {item.date}
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}

function EmptyState({ 
  icon, 
  title, 
  subtitle 
}: { 
  icon: React.ReactNode
  title: string
  subtitle: string 
}) {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '60px 20px',
      textAlign: 'center'
    }}>
      <div style={{ marginBottom: 16 }}>{icon}</div>
      <div style={{ fontSize: 16, color: '#666', marginBottom: 8 }}>{title}</div>
      <div style={{ fontSize: 14, color: '#999' }}>{subtitle}</div>
    </div>
  )
}
