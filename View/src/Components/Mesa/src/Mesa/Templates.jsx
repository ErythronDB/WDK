import React from 'react';

import { OptionsDefaults } from 'Mesa/Defaults';
import OverScroll from 'Mesa/Components/OverScroll';
import TruncatedText from 'Mesa/Components/TruncatedText';
import Utils from 'Mesa/Utils/Utils';

const Templates = {
  cell (column, row) {
    const { key, truncated } = column;
    if (!key) return;

    const className = 'Cell Cell-' + key;
    const value = row[key];
    const text = Utils.stringValue(value);

    return truncated
      ? <TruncatedText className={className} cutoff={truncated ? OptionsDefaults.overflowHeight : null} text={text} />
      : <div className={className}>{text}</div>
  },

  htmlCell (column, row) {
    const { key, truncated } = column;
    if (!key) return;

    const className = 'Cell HtmlCell Cell-' + key;
    const content = (<div dangerouslySetInnerHTML={{ __html: row[key] }} />);
    const size = (truncated === true ? '16em' : truncated);

    return truncated
      ? <OverScroll className={className} size={size}>{content}</OverScroll>
      : <div className={className}>{content}</div>
  },

  heading (column) {
    let { key, name } = column;
    if (!key) return;

    const className = 'Cell Heading-Cell Heading-Cell-' + key;
    const content = (<b>{name || key}</b>);

    return (
      <div className={className}>
        {content}
      </div>
    )
  }
};

export default Templates;
