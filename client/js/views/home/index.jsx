import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { Row } from 'react-bootstrap';



class Home extends Component {
  componentDidMount() {
  }
  
  render() {
    const { } = this.props;
    
    return (
      <div id="video-lists" className="body-content container">
			<p>home content</p>
      </div>
    );
  }
}

// Prop validation
Home.propTypes = {
  // State from redux store
};

function mapStateToProps(state) {
  const { } = state;
  return { };
}

function mapDispatchToProps(dispatch) {
  return {
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Home);