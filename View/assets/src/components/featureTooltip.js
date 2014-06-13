wdk.namespace('wdk.components', function(ns, $) {
  'use strict';

  // Creates a feature tooltip.
  //
  // opts:
  //   * el is the tooltip target
  //   * featureType is the type of feature
  //   * title is the title of the tooltip (see http://qtip2.com/options#content.title)
  //   * text is the content of the tooltip (see http://qtip2.com/options#content.text)
  ns.createFeatureTooltip = function createFeatureTooltip(opts) {
    var $el = opts.el,
        key = opts.key,
        title = opts.title,
        text = opts.text,

        dismissedStorageKey = 'featureTooltip::dismissed::' + key;

    ($el instanceof $) || ($el = $($el));

    if (localStorage.getItem(dismissedStorageKey) == true)
      return;

    return $el
      .wdkTooltip({
        content: {
          text: text,
          title: '<img title="This is a new search!" alt="New feature icon" ' +
                 'src="' + wdk.assetsUrl('/wdk/images/new-feature.png') + '"> ' +
                 title,
          button: 'Got it!'
        },
        style: {
          classes: 'qtip-bootstrap wdk-feature-tooltip'
        },
        position: {
          my: 'left top',
          at: 'right center'
        },
        hide: {
          event: false
        },
        show: {
          event: false,
          ready: true
        },
        events: {
          hide: function(e, api) {
            if (e.originalEvent.type === 'tooltipsolo') {
              e.preventDefault();
            } else {
              localStorage.setItem(dismissedStorageKey, 1);
              api.destroy();
            }
          }
        }
      });
  }

});
