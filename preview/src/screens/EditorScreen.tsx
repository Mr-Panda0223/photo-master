import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Undo2, Redo2, Crop, SlidersHorizontal, Sparkles, Sun, Type, Brush, Image as ImageIcon } from 'lucide-react'

type ToolType = 'none' | 'crop' | 'adjust' | 'filter' | 'light' | 'text' | 'brush'

const tools = [
  { type: 'crop' as ToolType, name: '裁剪', icon: Crop },
  { type: 'adjust' as ToolType, name: '调整', icon: SlidersHorizontal },
  { type: 'filter' as ToolType, name: '滤镜', icon: Sparkles },
  { type: 'light' as ToolType, name: '光影', icon: Sun },
  { type: 'text' as ToolType, name: '文字', icon: Type },
  { type: 'brush' as ToolType, name: '画笔', icon: Brush },
]

export default function EditorScreen() {
  const navigate = useNavigate()
  const { imageId } = useParams()
  const [selectedTool, setSelectedTool] = useState<ToolType>('none')
  const [canUndo] = useState(false)
  const [canRedo] = useState(false)

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: '#1a1a1a' }}>
      {/* Header */}
      <div style={{
        height: 56,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 8px',
        background: '#2d2d2d'
      }}>
        <button 
          onClick={() => navigate('/')}
          style={{ background: 'none', border: 'none', padding: 8, cursor: 'pointer' }}
        >
          <ArrowLeft size={24} color="white" />
        </button>
        
        <span style={{ fontSize: 18, color: 'white', fontWeight: 500 }}>编辑</span>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <button style={{ background: 'none', border: 'none', padding: 8, cursor: 'pointer', opacity: canUndo ? 1 : 0.3 }}>
            <Undo2 size={24} color="white" />
          </button>
          <button style={{ background: 'none', border: 'none', padding: 8, cursor: 'pointer', opacity: canRedo ? 1 : 0.3 }}>
            <Redo2 size={24} color="white" />
          </button>
          <button 
            onClick={() => navigate('/export')}
            style={{ 
              background: '#00d4aa', 
              border: 'none', 
              padding: '8px 16px', 
              borderRadius: 8,
              cursor: 'pointer',
              color: 'white',
              fontWeight: 500
            }}
          >
            导出
          </button>
        </div>
      </div>

      {/* Canvas Area */}
      <div style={{ 
        flex: 1, 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        padding: 16
      }}>
        <div style={{
          width: '90%',
          aspectRatio: '3/4',
          background: '#2d2d2d',
          borderRadius: 8,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>
          <ImageIcon size={64} color="#666" />
        </div>
      </div>

      {/* Tool Panel */}
      {selectedTool !== 'none' && (
        <div style={{
          background: 'white',
          borderRadius: '16px 16px 0 0',
          padding: 16,
          animation: 'slideUp 0.3s ease-out'
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <span style={{ fontSize: 16, fontWeight: 500 }}>
              {tools.find(t => t.type === selectedTool)?.name}
            </span>
            <button 
              onClick={() => setSelectedTool('none')}
              style={{ background: 'none', border: 'none', color: '#00d4aa', cursor: 'pointer', fontSize: 14 }}
            >
              完成
            </button>
          </div>
          
          {selectedTool === 'adjust' && <AdjustPanel />}
          {selectedTool === 'filter' && <FilterPanel />}
          {selectedTool === 'crop' && <CropPanel />}
          {selectedTool === 'light' && <LightPanel />}
          {selectedTool === 'text' && <TextPanel />}
          {selectedTool === 'brush' && <BrushPanel />}
        </div>
      )}

      {/* Bottom Toolbar */}
      <div style={{
        background: 'white',
        padding: '8px 0',
        borderTop: '1px solid #f0f0f0'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-evenly' }}>
          {tools.map(tool => {
            const Icon = tool.icon
            const isSelected = selectedTool === tool.type
            return (
              <div
                key={tool.type}
                onClick={() => setSelectedTool(isSelected ? 'none' : tool.type)}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 4,
                  cursor: 'pointer',
                  padding: '8px 12px'
                }}
              >
                <div style={{
                  width: 48,
                  height: 48,
                  borderRadius: 12,
                  background: isSelected ? '#e6faf5' : '#f5f5f5',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <Icon size={24} color={isSelected ? '#00d4aa' : '#666'} />
                </div>
                <span style={{ fontSize: 12, color: isSelected ? '#00d4aa' : '#666' }}>{tool.name}</span>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}

function AdjustPanel() {
  const params = [
    { name: '亮度', value: 0 },
    { name: '对比度', value: 0 },
    { name: '饱和度', value: 0 },
    { name: '色温', value: 0 },
    { name: '色调', value: 0 },
    { name: '曝光', value: 0 },
    { name: '高光', value: 0 },
    { name: '阴影', value: 0 },
  ]

  return (
    <div style={{ maxHeight: 300, overflow: 'auto' }}>
      {params.map(param => (
        <div key={param.name} style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
            <span style={{ fontSize: 14 }}>{param.name}</span>
            <span style={{ fontSize: 14, color: '#00d4aa' }}>{param.value}</span>
          </div>
          <input
            type="range"
            min="-100"
            max="100"
            defaultValue="0"
            style={{ width: '100%' }}
          />
        </div>
      ))}
    </div>
  )
}

function FilterPanel() {
  const filters = ['原图', '人像', '风景', '美食', '黑白', '胶片', '复古', '明亮']
  
  return (
    <div style={{ display: 'flex', gap: 12, overflow: 'auto', padding: '4px 0' }}>
      {filters.map(filter => (
        <div key={filter} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
          <div style={{ width: 72, height: 72, background: '#f5f5f5', borderRadius: 8 }} />
          <span style={{ fontSize: 12 }}>{filter}</span>
        </div>
      ))}
    </div>
  )
}

function CropPanel() {
  return (
    <div>
      <span style={{ fontSize: 14, color: '#666' }}>比例</span>
      <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
        {['自由', '1:1', '4:3', '16:9'].map(ratio => (
          <button
            key={ratio}
            style={{
              padding: '8px 16px',
              borderRadius: 16,
              border: '1px solid #e8e8e8',
              background: ratio === '自由' ? '#e6faf5' : 'white',
              color: ratio === '自由' ? '#00d4aa' : '#333',
              cursor: 'pointer'
            }}
          >
            {ratio}
          </button>
        ))}
      </div>
    </div>
  )
}

function LightPanel() {
  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
          <span style={{ fontSize: 14 }}>暗角</span>
          <span style={{ fontSize: 14, color: '#00d4aa' }}>0</span>
        </div>
        <input type="range" min="0" max="100" defaultValue="0" style={{ width: '100%' }} />
      </div>
      <div style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
          <span style={{ fontSize: 14 }}>光晕</span>
          <span style={{ fontSize: 14, color: '#00d4aa' }}>0</span>
        </div>
        <input type="range" min="0" max="100" defaultValue="0" style={{ width: '100%' }} />
      </div>
      <div style={{ display: 'flex', gap: 12 }}>
        <button style={{ flex: 1, padding: 12, border: '1px solid #e8e8e8', borderRadius: 8, background: 'white', cursor: 'pointer' }}>
          局部调亮
        </button>
        <button style={{ flex: 1, padding: 12, border: '1px solid #e8e8e8', borderRadius: 8, background: 'white', cursor: 'pointer' }}>
          局部调暗
        </button>
      </div>
    </div>
  )
}

function TextPanel() {
  return (
    <div>
      <button style={{ width: '100%', padding: 12, background: '#00d4aa', color: 'white', border: 'none', borderRadius: 8, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
        <span>+</span>
        <span>添加文字</span>
      </button>
      <div style={{ marginTop: 16 }}>
        <span style={{ fontSize: 14, color: '#666' }}>样式</span>
        <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
          {['粗体', '阴影', '描边'].map(style => (
            <button key={style} style={{ padding: '8px 16px', border: '1px solid #e8e8e8', borderRadius: 16, background: 'white', cursor: 'pointer' }}>
              {style}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

function BrushPanel() {
  return (
    <div>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
        {['画笔', '马克笔', '马赛克'].map(brush => (
          <button
            key={brush}
            style={{
              padding: '8px 16px',
              borderRadius: 16,
              border: '1px solid #e8e8e8',
              background: brush === '画笔' ? '#e6faf5' : 'white',
              color: brush === '画笔' ? '#00d4aa' : '#333',
              cursor: 'pointer'
            }}
          >
            {brush}
          </button>
        ))}
      </div>
      <div style={{ marginBottom: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
          <span style={{ fontSize: 14 }}>大小</span>
          <span style={{ fontSize: 14, color: '#00d4aa' }}>50</span>
        </div>
        <input type="range" min="0" max="100" defaultValue="50" style={{ width: '100%' }} />
      </div>
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
          <span style={{ fontSize: 14 }}>不透明度</span>
          <span style={{ fontSize: 14, color: '#00d4aa' }}>80</span>
        </div>
        <input type="range" min="0" max="100" defaultValue="80" style={{ width: '100%' }} />
      </div>
    </div>
  )
}
