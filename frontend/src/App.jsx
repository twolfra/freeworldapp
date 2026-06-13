import Navbar from './components/Navbar';
import Home from './pages/Home';
import OfferList from './pages/OfferList';
import OfferForm from './pages/OfferForm';
import RequestList from './pages/RequestList';
import RequestForm from './pages/RequestForm';
import OfferDetail from './pages/OfferDetail';
import RequestDetail from './pages/RequestDetail';
import Inbox from './pages/Inbox';
import Conversation from './pages/Conversation';
import UserProfile from './pages/UserProfile';
import Subscriptions from './pages/Subscriptions';
import Register from './pages/Register';
import Login from './pages/Login';
import VerifyEmail from './pages/VerifyEmail';

function resolve(path) {
  const exact = {
    '/': Home,
    '/offers': OfferList,
    '/offers/new': OfferForm,
    '/requests': RequestList,
    '/requests/new': RequestForm,
    '/messages': Inbox,
    '/subscriptions': Subscriptions,
    '/register': Register,
    '/login': Login,
    '/verify-email': VerifyEmail,
  };
  if (exact[path]) return { Page: exact[path], params: {} };

  const offerDetail = path.match(/^\/offers\/([^/]+)$/);
  if (offerDetail) return { Page: OfferDetail, params: { id: offerDetail[1] } };

  const requestDetail = path.match(/^\/requests\/([^/]+)$/);
  if (requestDetail) return { Page: RequestDetail, params: { id: requestDetail[1] } };

  const conversation = path.match(/^\/messages\/([^/]+)$/);
  if (conversation) return { Page: Conversation, params: { userId: conversation[1] } };

  const userProfile = path.match(/^\/users\/([^/]+)$/);
  if (userProfile) return { Page: UserProfile, params: { id: userProfile[1] } };

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
