import Navbar from './components/Navbar';
import Home from './pages/Home';
import OfferList from './pages/OfferList';
import Register from './pages/Register';

const routes = {
  '/': Home,
  '/offers': OfferList,
  '/register': Register,
};

export default function App() {
  const path = window.location.pathname;
  const Page = routes[path] ?? Home;

  return (
    <>
      <Navbar />
      <Page />
    </>
  );
}
