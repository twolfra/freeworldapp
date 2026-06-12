import Navbar from './components/Navbar';
import Home from './pages/Home';
import OfferList from './pages/OfferList';
import OfferForm from './pages/OfferForm';
import Register from './pages/Register';
import Login from './pages/Login';

const routes = {
  '/': Home,
  '/offers': OfferList,
  '/offers/new': OfferForm,
  '/register': Register,
  '/login': Login,
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
