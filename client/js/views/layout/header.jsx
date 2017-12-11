import React, { Component, PropTypes } from 'react';
import { Link } from 'react-router';
import { connect } from 'react-redux';
import { routeActions } from 'react-router-redux';
import { Navbar, Nav, NavItem, NavDropdown, MenuItem } from 'react-bootstrap';

//import { toggleWhatIsThis } from 'actions';
//import { toggleTour } from 'actions';
//import { getCurrentUser } from 'actions/authentication';
//import { getSuggestions } from 'actions/search';
import Icon from 'components/shared/icon';
import UserProfileImage from 'components/users/user-profile-image';
import WhatIsThis from './what-is-this';
import SearchBox from './search-box';
import Tour from 'components/shared/tour';
//import { logout } from 'actions/authentication';


var logoUrl = require('killrvideo.png');

class Header extends Component {
  componentDidMount() {
    
  }
  
  submitSearch(q) {
      this.props.push({
        pathname: '/search/results', 
        query: { q }
      });
  }
  
  render() {
    // Leave these undefined if we haven't gotten the current user information from the server yet
     
    
    return (
         <div id="header">
            <Navbar fixedTop id="navbar-main">
              <Navbar.Header>
                <Navbar.Brand>
                  <Link to="/" id="logo">
                    <img src={logoUrl} alt="KillrVideo.com Logo" />
                  </Link>
                </Navbar.Brand>
                <Navbar.Toggle />
              </Navbar.Header>
              <Navbar.Collapse>
                <Nav navbar pullRight> 
                  <NavItem id="show-tour" eventKey={1} href="#" onSelect={e => this.startTour(this.props.currentUser.isLoggedIn)} >
                    <Icon name="map-signs" fixedWidth /> Tour: <span>{this.props.showTour ? 'On' : 'Off'}</span>
                  </NavItem> 
                  <NavItem eventKey={1} href="#" onSelect={e => this.props.toggleWhatIsThis()} className={this.props.showWhatIsThis ? 'dropup' : ''}>
                    What is this? <span className="caret"></span>
                  </NavItem>
                </Nav>
              </Navbar.Collapse>
              
            </Navbar>
            
            <WhatIsThis showWhatIsThis={this.props.showWhatIsThis} toggleWhatIsThis={this.props.toggleWhatIsThis} />
            <Tour showTour={this.props.showTour} toggleTour={this.props.toggleTour} />
         </div>
    );
  }
}

// Prop validation
Header.propTypes = {
};

// Falcor queries
Header.queries = {
};

function mapStateToProps(state) {
  const { } = state;
  return {
    
  };
}

export default Header;