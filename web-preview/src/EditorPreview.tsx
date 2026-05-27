import { useState } from 'react';
import {
  ArrowLeft,
  Undo,
  Redo,
  Crop,
  RotateCcw,
  FlipHorizontal,
  SlidersHorizontal,
  Palette,
  Type,
  Smile,
  Brush,
  Plus,
  Check
} from 'lucide-react';

interface EditorPreviewProps {
  onNavigateBack: () => void;
}

type ToolType = 'none' | 'crop' | 'rotate' | 'flip' | 'adjust' | 'filter' | 'text' | 'sticker' | 'brush';

export default function EditorPreview({ onNavigateBack }: EditorPreviewProps) {
  const [currentTool, setCurrentTool] = useState<ToolType>('none');
  const [canUndo, setCanUndo] = useState(true);
  const [canRedo, setCanRedo] = useState(false);
  const [brightness, setBrightness] = useState(0);
  const [contrast, setContrast] = useState(1);
  const [saturation, setSaturation] = useState(1);

  const filters = ['原图', '黑白', '复古', '清新', '暖色', '冷色', '胶片', '美食'];
  const stickers = ['😀', '😍', '🎉', '❤️', '⭐', '🔥', '🌈', '🎨'];
  const brushColors = ['#000000', '#FFFFFF', '#FF0000', '#00FF00', '#0000FF', '#FFFF00', '#FF00FF', '#00FFFF'];

  return (
    <div style={{
      width: '100%',
      height: '100vh',
      background: '#000',
      display: 'flex',
      flexDirection: 'column',
      maxWidth: '430px',
      margin: '0 auto',
      boxShadow: '0 0 20px rgba(0,0,0,0.3)'
    }}>
      {/* Header */}
      <div style={{
        background: '#1a1a1a',
        padding: '12px 16px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottom: '1px solid #333'
      }}>
        <button
          onClick={onNavigateBack}
          style={{
            background: 'transparent',
            border: 'none',
            color: 'white',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '4px'
          }}
        >
          <ArrowLeft size={24} />
        </button>
        <span style={{ color: 'white', fontSize: '18px', fontWeight: 'bold' }}>编辑</span>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            onClick={() => setCanUndo(!canUndo)}
            style={{
              background: 'transparent',
              border: 'none',
              color: canUndo ? '#667eea' : '#666',
              cursor: 'pointer',
              padding: '8px'
            }}
          >
            <Undo size={22} />
          </button>
          <button
            onClick={() => setCanRedo(!canRedo)}
            style={{
              background: 'transparent',
              border: 'none',
              color: canRedo ? '#667eea' : '#666',
              cursor: 'pointer',
              padding: '8px'
            }}
          >
            <Redo size={22} />
          </button>
          <button
            style={{
              background: '#667eea',
              border: 'none',
              color: 'white',
              padding: '8px 16px',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: 'bold'
            }}
          >
            导出
          </button>
        </div>
      </div>

      {/* Image Preview Area */}
      <div style={{
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '16px',
        background: '#000'
      }}>
        <div style={{
          width: '100%',
          height: '100%',
          background: '#222',
          borderRadius: '8px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          position: 'relative',
          overflow: 'hidden'
        }}>
          {/* Placeholder Image */}
          <div style={{
            width: '80%',
            height: '60%',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            borderRadius: '8px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '80px'
          }}>
            🏔️
          </div>
        </div>
      </div>

      {/* Tools Panel */}
      <div style={{
        background: '#1a1a1a',
        borderTop: '1px solid #333'
      }}>
        {/* Tool Options Area */}
        <div style={{
          padding: '16px',
          minHeight: '100px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>
          {currentTool === 'none' && (
            <span style={{ color: '#666', fontSize: '14px' }}>选择下方工具开始编辑</span>
          )}

          {currentTool === 'crop' && (
            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', justifyContent: 'center' }}>
              {['自由', '1:1', '4:3', '16:9', '3:4'].map((ratio) => (
                <button
                  key={ratio}
                  style={{
                    padding: '8px 16px',
                    background: '#333',
                    border: '1px solid #444',
                    borderRadius: '8px',
                    color: 'white',
                    cursor: 'pointer'
                  }}
                >
                  {ratio}
                </button>
              ))}
            </div>
          )}

          {currentTool === 'rotate' && (
            <div style={{ display: 'flex', gap: '16px' }}>
              <button style={{
                padding: '10px 20px',
                background: '#333',
                border: '1px solid #444',
                borderRadius: '8px',
                color: 'white',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}>
                <RotateCcw size={18} />
                向左90°
              </button>
              <button style={{
                padding: '10px 20px',
                background: '#333',
                border: '1px solid #444',
                borderRadius: '8px',
                color: 'white',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}>
                <RotateCcw size={18} style={{ transform: 'scaleX(-1)' }} />
                向右90°
              </button>
            </div>
          )}

          {currentTool === 'flip' && (
            <div style={{ display: 'flex', gap: '16px' }}>
              <button style={{
                padding: '10px 20px',
                background: '#333',
                border: '1px solid #444',
                borderRadius: '8px',
                color: 'white',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}>
                <FlipHorizontal size={18} />
                水平翻转
              </button>
              <button style={{
                padding: '10px 20px',
                background: '#333',
                border: '1px solid #444',
                borderRadius: '8px',
                color: 'white',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}>
                <FlipHorizontal size={18} style={{ transform: 'rotate(90deg)' }} />
                垂直翻转
              </button>
            </div>
          )}

          {currentTool === 'adjust' && (
            <div style={{ width: '100%', padding: '0 16px' }}>
              <div style={{ marginBottom: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'white', marginBottom: '4px' }}>
                  <span>亮度</span>
                  <span>{brightness}</span>
                </div>
                <input
                  type="range"
                  min="-100"
                  max="100"
                  value={brightness}
                  onChange={(e) => setBrightness(parseInt(e.target.value))}
                  style={{ width: '100%' }}
                />
              </div>
              <div style={{ marginBottom: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'white', marginBottom: '4px' }}>
                  <span>对比度</span>
                  <span>{contrast.toFixed(1)}</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="2"
                  step="0.1"
                  value={contrast}
                  onChange={(e) => setContrast(parseFloat(e.target.value))}
                  style={{ width: '100%' }}
                />
              </div>
              <div style={{ marginBottom: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'white', marginBottom: '4px' }}>
                  <span>饱和度</span>
                  <span>{saturation.toFixed(1)}</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="2"
                  step="0.1"
                  value={saturation}
                  onChange={(e) => setSaturation(parseFloat(e.target.value))}
                  style={{ width: '100%' }}
                />
              </div>
              <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', marginTop: '16px' }}>
                <button
                  onClick={() => { setBrightness(0); setContrast(1); setSaturation(1); }}
                  style={{
                    padding: '8px 24px',
                    background: 'transparent',
                    border: '1px solid #667eea',
                    borderRadius: '8px',
                    color: '#667eea',
                    cursor: 'pointer'
                  }}
                >
                  重置
                </button>
                <button
                  style={{
                    padding: '8px 24px',
                    background: '#667eea',
                    border: 'none',
                    borderRadius: '8px',
                    color: 'white',
                    cursor: 'pointer'
                  }}
                >
                  应用
                </button>
              </div>
            </div>
          )}

          {currentTool === 'filter' && (
            <div style={{ display: 'flex', gap: '12px', overflowX: 'auto', padding: '8px' }}>
              {filters.map((filter) => (
                <div
                  key={filter}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: '4px',
                    cursor: 'pointer'
                  }}
                >
                  <div style={{
                    width: '64px',
                    height: '64px',
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    borderRadius: '8px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    border: filter === '原图' ? '2px solid #667eea' : '2px solid transparent'
                  }}>
                    <Palette size={24} color="white" />
                  </div>
                  <span style={{ color: 'white', fontSize: '12px' }}>{filter}</span>
                </div>
              ))}
            </div>
          )}

          {currentTool === 'text' && (
            <button style={{
              padding: '12px 24px',
              background: '#667eea',
              border: 'none',
              borderRadius: '8px',
              color: 'white',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              <Plus size={20} />
              添加文字
            </button>
          )}

          {currentTool === 'sticker' && (
            <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', justifyContent: 'center' }}>
              {stickers.map((sticker) => (
                <span
                  key={sticker}
                  style={{
                    fontSize: '32px',
                    cursor: 'pointer',
                    padding: '8px'
                  }}
                >
                  {sticker}
                </span>
              ))}
            </div>
          )}

          {currentTool === 'brush' && (
            <div>
              <p style={{ color: 'white', marginBottom: '12px', textAlign: 'center' }}>画笔颜色</p>
              <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
                {brushColors.map((color) => (
                  <div
                    key={color}
                    style={{
                      width: '32px',
                      height: '32px',
                      background: color,
                      borderRadius: '50%',
                      cursor: 'pointer',
                      border: '2px solid #666'
                    }}
                  />
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Divider */}
        <div style={{ height: '1px', background: '#333' }} />

        {/* Tool Icons Row */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-around',
          padding: '12px 8px',
          overflowX: 'auto'
        }}>
          <ToolIcon
            icon={<Crop size={22} />}
            label="裁剪"
            isSelected={currentTool === 'crop'}
            onClick={() => setCurrentTool('crop')}
          />
          <ToolIcon
            icon={<RotateCcw size={22} />}
            label="旋转"
            isSelected={currentTool === 'rotate'}
            onClick={() => setCurrentTool('rotate')}
          />
          <ToolIcon
            icon={<FlipHorizontal size={22} />}
            label="翻转"
            isSelected={currentTool === 'flip'}
            onClick={() => setCurrentTool('flip')}
          />
          <ToolIcon
            icon={<SlidersHorizontal size={22} />}
            label="调整"
            isSelected={currentTool === 'adjust'}
            onClick={() => setCurrentTool('adjust')}
          />
          <ToolIcon
            icon={<Palette size={22} />}
            label="滤镜"
            isSelected={currentTool === 'filter'}
            onClick={() => setCurrentTool('filter')}
          />
          <ToolIcon
            icon={<Type size={22} />}
            label="文字"
            isSelected={currentTool === 'text'}
            onClick={() => setCurrentTool('text')}
          />
          <ToolIcon
            icon={<Smile size={22} />}
            label="贴纸"
            isSelected={currentTool === 'sticker'}
            onClick={() => setCurrentTool('sticker')}
          />
          <ToolIcon
            icon={<Brush size={22} />}
            label="画笔"
            isSelected={currentTool === 'brush'}
            onClick={() => setCurrentTool('brush')}
          />
        </div>
      </div>
    </div>
  );
}

function ToolIcon({
  icon,
  label,
  isSelected,
  onClick
}: {
  icon: React.ReactNode;
  label: string;
  isSelected: boolean;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '4px',
        padding: '8px',
        background: 'transparent',
        border: 'none',
        cursor: 'pointer',
        color: isSelected ? '#667eea' : '#999',
        minWidth: '50px'
      }}
    >
      <div style={{
        width: '44px',
        height: '44px',
        borderRadius: '50%',
        background: isSelected ? 'rgba(102, 126, 234, 0.2)' : 'transparent',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        border: isSelected ? '2px solid #667eea' : '2px solid transparent'
      }}>
        {icon}
      </div>
      <span style={{ fontSize: '11px' }}>{label}</span>
    </button>
  );
}
