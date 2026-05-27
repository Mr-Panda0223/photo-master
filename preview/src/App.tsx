import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import HomeScreen from './screens/HomeScreen'
import ProfileScreen from './screens/ProfileScreen'
import EditorScreen from './screens/EditorScreen'
import ExportScreen from './screens/ExportScreen'

function App() {
  return (
    <div className="mobile-frame">
      <Router>
        <Routes>
          <Route path="/" element={<MainLayout />}>
            <Route index element={<HomeScreen />} />
            <Route path="profile" element={<ProfileScreen />} />
          </Route>
          <Route path="/editor/:imageId" element={<EditorScreen />} />
          <Route path="/export" element={<ExportScreen />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </div>
  )
}

export default App
