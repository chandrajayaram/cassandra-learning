import React from 'react';
import { Router, IndexRoute, Route, browserHistory } from 'react-router';
import * as Views from 'views';

export default (
  <Router history={browserHistory}>
    <Route path="/" component={Views.Layout}>
      <IndexRoute component={Views.Home} />
    </Route>
  </Router>
);