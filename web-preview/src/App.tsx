import { useState } from 'react';
import EditorPreview from './EditorPreview';
import HomePreview from './HomePreview';

type Screen = 'home' | 'editor';

function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');

  return (
    <div style={{ width: '100%', height: '100vh' }}>
      {currentScreen === 'home' ? (
        <HomePreview onNavigateToEditor={() => setCurrentScreen('editor')} />
      ) : (
        <EditorPreview onNavigateBack={() => setCurrentScreen('home')} />
      )}
    </div>
  );
}

export default App;
