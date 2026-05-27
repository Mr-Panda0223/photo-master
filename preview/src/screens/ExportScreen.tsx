import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Image as ImageIcon } from 'lucide-react'

export default function ExportScreen() {
  const navigate = useNavigate()
  const [selectedFormat, setSelectedFormat] = useState('JPEG')
  const [quality, setQuality] = useState(90)
  const [isSaving, setIsSaving] = useState(false)

  const handleSave = () => {
    setIsSaving(true)
    setTimeout(() => {
      setIsSaving(false)
      navigate('/')
    }, 1500)
  }

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
        <span style={{ fontSize: 18, fontWeight: 500, marginLeft: 8 }}>导出</span>
      </div>

      {/* Content */}
      <div style={{ flex: 1, padding: 16, overflow: 'auto' }}>
        {/* Preview */}
        <div style={{
          background: '#f5f5f5',
          borderRadius: 12,
          padding: 24,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: 8
        }}>
          <div style={{
            width: '80%',
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

        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <span style={{ fontSize: 12, color: '#999' }}>1920 x 1080  •  预估 2.5MB</span>
        </div>

        {/* Format */}
        <div style={{ marginBottom: 16 }}>
          <span style={{ fontSize: 14, color: '#666' }}>格式</span>
          <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
            {['JPEG', 'PNG'].map(format => (
              <button
                key={format}
                onClick={() => setSelectedFormat(format)}
                style={{
                  flex: 1,
                  padding: '12px',
                  borderRadius: 8,
                  border: '1px solid #e8e8e8',
                  background: selectedFormat === format ? '#e6faf5' : 'white',
                  color: selectedFormat === format ? '#00d4aa' : '#333',
                  cursor: 'pointer',
                  fontWeight: selectedFormat === format ? 500 : 400
                }}
              >
                {format}
              </button>
            ))}
          </div>
        </div>

        {/* Quality */}
        {selectedFormat === 'JPEG' && (
          <div style={{ marginBottom: 16 }}>
            <span style={{ fontSize: 14, color: '#666' }}>质量: {quality}%</span>
            <input
              type="range"
              min="10"
              max="100"
              value={quality}
              onChange={(e) => setQuality(Number(e.target.value))}
              style={{ width: '100%', marginTop: 8 }}
            />
          </div>
        )}

        {/* Size */}
        <div style={{ marginBottom: 32 }}>
          <span style={{ fontSize: 14, color: '#666' }}>尺寸</span>
          <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
            <button style={{
              flex: 1,
              padding: '12px',
              borderRadius: 8,
              border: '1px solid #e8e8e8',
              background: '#e6faf5',
              color: '#00d4aa',
              cursor: 'pointer',
              fontWeight: 500
            }}>
              原始
            </button>
            <button style={{
              flex: 1,
              padding: '12px',
              borderRadius: 8,
              border: '1px solid #e8e8e8',
              background: 'white',
              color: '#333',
              cursor: 'pointer'
            }}>
              自定义
            </button>
          </div>
        </div>

        {/* Save Button */}
        <button
          onClick={handleSave}
          disabled={isSaving}
          style={{
            width: '100%',
            padding: '14px',
            background: '#00d4aa',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            fontSize: 16,
            fontWeight: 500,
            cursor: isSaving ? 'not-allowed' : 'pointer',
            opacity: isSaving ? 0.7 : 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          {isSaving ? (
            <div style={{
              width: 20,
              height: 20,
              border: '2px solid white',
              borderTopColor: 'transparent',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite'
            }} />
          ) : (
            '保存到相册'
          )}
        </button>
      </div>

      <style>{`
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  )
}
