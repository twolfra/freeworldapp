import Navbar from './components/Navbar';
import Home from './pages/Home';
import OfferList from './pages/OfferList';
import OfferForm from './pages/OfferForm';
import RequestList from './pages/RequestList';
import RequestForm from './pages/RequestForm';
import OfferDetail from './pages/OfferDetail';
import RequestDetail from './pages/RequestDetail';
import Register from './pages/Register';
import Login from './pages/Login';

function resolve(path) {
  const exact = {
    '/': Home,
    '/offers': OfferList,
    '/offers/new': OfferForm,
    '/requests': RequestList,
    '/requests/new': RequestForm,
    '/register': Register,
    '/login': Login,
  };
  if (exact[path]) return { Page: exact[path], params: {} };

  const offerDetail = path.match(/^\/offers\/([^/]+)$/);
  if (offerDetail) return { Page: OfferDetail, params: { id: offerDetail[1] } };

  const requestDetail = path.match(/^\/requests\/([^/]+)$/);
  if (requestDetail) return { Page: RequestDetail, params: { id: requestDetail[1] } };

  return { Page: Home, params: {} };
}

export default function App() {
  const { Page, params } = resolve(window.location.pathname);

  return (
    <>
      <Navbar />
      <Page {...params} />
    </>
  );
}
