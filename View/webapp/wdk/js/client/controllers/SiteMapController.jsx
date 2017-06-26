import { wrappable } from '../utils/componentUtils';
import WdkViewController from './WdkViewController';
import * as SiteMapActionCreators from '../actioncreators/SiteMapActionCreators';
import SiteMap from '../components/SiteMap';

class SiteMapController extends WdkViewController {

  getStoreName() {
    return "SiteMapStore";
  }

  getActionCreators() {
    return SiteMapActionCreators;
  }

  isRenderDataLoaded(state) {
    return (state.tree != null && !state.isLoading);
  }

  getTitle() {
    return "Data Finder";
  }

  renderView(state, eventHandlers) {
    return ( <SiteMap {...state} siteMapActions={eventHandlers}/> );
  }

  loadData(actionCreators, state) {
    if (state.tree == null) {
      actionCreators.loadCurrentSiteMap();
    }
  }

}

export default wrappable(SiteMapController);
