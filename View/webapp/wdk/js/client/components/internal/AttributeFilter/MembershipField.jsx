import React from 'react';
import { bindAll, escapeRegExp, get, has, isFunction, memoize } from 'lodash';
import { MesaController as Mesa, ModalBoundary } from 'mesa';
import 'mesa/dist/css/mesa.css';

import { instrument, safeHtml } from '../../../utils/componentUtils';
import RealTimeSearchBox from '../../RealTimeSearchBox';
import Toggle from '../../Toggle';
import FieldFilter from './FieldFilter';
import FilterLegend from './FilterLegend';

const UNKNOWN_ELEMENT = <em>Not specified</em>;

/**
 * Membership field component
 */
class MembershipField extends React.Component {
  static getHelpContent(props) {
    var displayName = props.displayName;
    var fieldDisplay = props.field.display;
    return (
      <div>
        You may add or remove {displayName} with specific {fieldDisplay} values
        from your overall selection by checking or unchecking the corresponding
        checkboxes.
      </div>
    );
  }

  constructor(props) {
    super(props);
    bindAll(this,
      'deriveRowClassName',
      'handleGroupBySelected',
      'handleRemoveAll',
      'handleRowClick',
      'handleRowDeselect',
      'handleRowSelect',
      'handleSearchTermChange',
      'handleSelectAll',
      'handleSort',
      'isItemSelected',
      'renderDistributionCell',
      'renderFilteredCountCell',
      'renderFilteredCountHeading1',
      'renderFilteredCountHeading2',
      'renderPrecentageCell',
      'renderUnfilteredCountCell',
      'renderUnfilteredCountHeading1',
      'renderUnfilteredCountHeading2',
      'renderValueCell',
      'renderValueHeading',
      'renderValueHeadingSearch',
      'toFilterValue',
    );
    this.getKnownValues = memoize(this.getKnownValues);
    this.isItemSelected = memoize(this.isItemSelected);
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.fieldSummary !== nextProps.fieldSummary) {
      this.getKnownValues.cache.clear();
    }
    if (this.props.filter !== nextProps.filter) {
      this.isItemSelected.cache.clear();
    }
  }

  toFilterValue(value) {
    return this.props.field.type === 'string' ? String(value)
      : this.props.field.type === 'number' ? Number(value)
      : this.props.field.type === 'date' ? Date(value)
      : value;
  }

  getKnownValues() {
    return this.props.fieldSummary.valueCounts
      .filter(({ value }) => value != null)
      .map(({ value }) => value);
  }

  getValuesForFilter() {
    return get(this.props, 'filter.value');
  }

  getFilteredRows(searchTerm) {
    let re = new RegExp(escapeRegExp(searchTerm), 'i');
    return searchTerm !== ''
      ? this.props.fieldSummary.valueCounts.filter(entry => re.test(entry.value))
      : this.props.fieldSummary.valueCounts;
  }

  deriveRowClassName(item) {
    return 'member' +
      (item.filteredCount === 0 ? ' member__disabled' : '') +
      (this.isItemSelected(item) ? ' member__selected' : '')
  }

  isItemSelected(item) {
    let { filter, selectByDefault } = this.props;

    return filter == null ? selectByDefault
      // value is null (ie, unknown) and includeUnknown selected
      : item.value == null ? filter.includeUnknown
      // filter.value is null (ie, all known values), or filter.value includes value
      : filter.value == null || filter.value.includes(item.value);
  }

  isSortEnabled() {
    return (
      has(this.props, 'fieldState.sort') &&
      isFunction(this.props.onSort)
    );
  }

  isSearchEnabled() {
    return (
      this.props.fieldSummary.valueCounts.length > 10 &&
      has(this.props, 'fieldState.searchTerm') &&
      isFunction(this.props.onSearch)
    );
  }

  handleItemClick(item, addItem = !this.isItemSelected(item)) {
    let { selectByDefault } = this.props;
    let { value } = item;
    if (value == null) {
      this.handleUnknownChange(addItem);
    }
    else {
      const currentFilterValue = this.props.filter == null
        ? (selectByDefault ? this.getKnownValues() : [])
        : this.getValuesForFilter() || this.getKnownValues();
      const filterValue = addItem
        ? currentFilterValue.concat(value)
        : currentFilterValue.filter(v => v !== value);

      this.emitChange(
        filterValue.length === this.getKnownValues().length ? undefined : filterValue,
        get(this.props, 'filter.includeUnknown', true)
      );
    }
  }

  handleRowClick(item) {
    this.handleItemClick(item);
  }

  handleRowSelect(item) {
    this.handleItemClick(item, true);
  }

  handleRowDeselect(item) {
    this.handleItemClick(item, false);
  }

  handleUnknownChange(addUnknown) {
    this.emitChange(this.getValuesForFilter(), addUnknown);
  }

  handleSelectAll() {
    const value = this.isSearchEnabled()
      ? this.getFilteredRows(this.props.fieldState.searchTerm).map(entry => entry.value)
      : undefined;
    this.emitChange(value, true);
  }

  handleRemoveAll() {
    this.emitChange([], false);
  }

  handleSort(rowData, direction) {
    let nextSort = { key: rowData.columnKey, direction };
    let sort = Object.assign({}, this.props.fieldState.sort, nextSort);
    this.props.onSort(this.props.field, sort);
  }

  handleGroupBySelected() {
    this.props.onSort(
      this.props.field,
      Object.assign({}, this.props.fieldState.sort, {
        groupBySelected: !this.props.fieldState.sort.groupBySelected
      })
    );
  }

  handleSearchTermChange(searchTerm) {
    this.props.onSearch(this.props.field, searchTerm);
  }

  emitChange(value, includeUnknown) {
    this.props.onChange(this.props.field, value, includeUnknown,
      this.props.fieldSummary.valueCounts);
  }

  renderValueHeading() {
    return this.props.field.display;
  }

  renderValueHeadingSearch() {
    return (
      <div
        style={{
          width: '15em',
          fontSize: '.8em',
          fontWeight: 'normal',
        }}
        onMouseUp={event => {
          event.stopPropagation();
        }}
      >
        <RealTimeSearchBox
          searchTerm={this.props.fieldState.searchTerm}
          placeholderText="Filter values"
          onSearchTermChange={this.handleSearchTermChange}
        />
      </div>
    );
  }

  renderValueCell({ value }) {
    return (
      <div>{value == null ? UNKNOWN_ELEMENT : safeHtml(String(value))}</div>
    );
  }

  renderCountHeading1(qualifier) {
    return (
      <div style={{display: 'flex', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
        <div>{qualifier}</div>
        <div style={{marginLeft: '.6ex', maxWidth: '6em', overflow: 'hidden', textOverflow: 'ellipsis'}}>{this.props.displayName}</div>
      </div>
    );
  }

  renderCountHeading2(internalsCount) {
    return (
      <div>
        {internalsCount.toLocaleString()}
        <small style={{ display: 'inline-block', width: '50%', textAlign: 'center' }}>(100%)</small>
      </div>
    );
  }

  renderCountCell(value, internalsCount) {
    return (
      <div>
        {value.toLocaleString()}
        &nbsp;
        {internalsCount && (
          <small style={{ display: 'inline-block', width: '50%', textAlign: 'center' }}>
            ({Math.round(value/internalsCount * 100)}%)
          </small>
        )}
      </div>
    );
  }

  renderFilteredCountHeading1() {
    return this.renderCountHeading1('Matching');
  }

  renderFilteredCountHeading2() {
    return this.renderCountHeading2(this.props.fieldSummary.internalsFilteredCount);
  }

  renderFilteredCountCell({ value }) {
    return this.renderCountCell(value, this.props.fieldSummary.internalsFilteredCount);
  }

  renderUnfilteredCountHeading1() {
    return this.renderCountHeading1('All');
  }

  renderUnfilteredCountHeading2() {
    return this.renderCountHeading2(this.props.fieldSummary.internalsCount);
  }

  renderUnfilteredCountCell({ value }) {
    return this.renderCountCell(value, this.props.fieldSummary.internalsCount);
  }

  renderDistributionCell({ row }) {
    return (
      <div className="bar">
        <div className="fill" style={{
          width: (row.count / (this.props.fieldSummary.internalsCount || this.props.dataCount) * 100) + '%'
        }}/>
        <div className="fill filtered" style={{
          width: (row.filteredCount / (this.props.fieldSummary.internalsCount || this.props.dataCount) * 100) + '%'
        }}/>
      </div>
    );
  }

  renderPrecentageCell({ row }) {
    return (
      <small title={`Matching ${row.value} / All ${row.value}`}>
        ({Math.round(row.filteredCount / row.count * 100)}%)
      </small>
    );
  }

  render() {
    var useSort = this.isSortEnabled();
    var useSearch = this.isSearchEnabled();

    var rows = useSearch
      ? this.getFilteredRows(this.props.fieldState.searchTerm)
      : this.props.fieldSummary.valueCounts;

    return (
      <ModalBoundary>
        <div className="membership-filter">
          { useSort ? (
            <div className="membership-actions">
              <div className="membership-action membership-action__group-selected">
                <button
                  style={{
                    background: 'none',
                    border: 'none'
                  }}
                  type="button"
                  onClick={this.handleGroupBySelected}
                >
                  <Toggle
                    on={this.props.fieldState.sort.groupBySelected}
                  /> Keep selected values at top
               </button>
              </div>
            </div>
          ) : null }

          <Mesa
            options={{
              isRowSelected: this.isItemSelected,
              deriveRowClassName: this.deriveRowClassName,
              onRowClick: this.handleRowClick,
              useStickyHeader: true,
              tableBodyMaxHeight: '80vh'
            }}
            uiState={this.props.fieldState}
            actions={[]}
            eventHandlers={{
              onRowSelect: this.handleRowSelect,
              onRowDeselect: this.handleRowDeselect,
              onMultipleRowSelect: this.handleSelectAll,
              onMultipleRowDeselect: this.handleRemoveAll,
              onSort: this.handleSort
            }}
            rows={this.props.fieldSummary.valueCounts}
            filteredRows={rows}
            columns={[
              {
                key: 'value',
                inline: true,
                sortable: useSort,
                wrapCustomHeadings: ({ headingRowIndex }) => headingRowIndex === 0,
                renderHeading: useSearch
                  ? [ this.renderValueHeading, this.renderValueHeadingSearch ]
                  : this.renderValueHeading,
                renderCell: this.renderValueCell
              },
              {
                key: 'filteredCount',
                sortable: useSort,
                width: '11em',
                helpText: (
                  <div>
                    The number of <em>{this.props.displayName}</em> that match the criteria chosen for other qualities, <br/>
                    and that have the given <em>{this.props.field.display}</em> value.
                  </div>
                ),
                wrapCustomHeadings: ({ headingRowIndex }) => headingRowIndex === 0,
                renderHeading: this.props.fieldSummary.internalsFilteredCount
                  ? [ this.renderFilteredCountHeading1, this.renderFilteredCountHeading2 ]
                  : this.renderFilteredCountHeading1,
                renderCell: this.renderFilteredCountCell
              },
              {
                key: 'count',
                sortable: useSort,
                width: '11em',
                helpText: (
                  <div>
                    The number of <em>{this.props.displayName}</em> with the
                    given <em>{this.props.field.display}</em> value.
                  </div>
                ),
                wrapCustomHeadings: ({ headingRowIndex }) => headingRowIndex === 0,
                renderHeading: this.props.fieldSummary.internalsCount
                  ? [ this.renderUnfilteredCountHeading1, this.renderUnfilteredCountHeading2 ]
                  : this.renderUnfilteredCountHeading1,
                renderCell: this.renderUnfilteredCountCell
              },
              {
                key: 'distribution',
                name: 'Distribution',
                width: '30%',
                helpText: <FilterLegend {...this.props} />,
                renderCell: this.renderDistributionCell
              },
              {
                key: '%',
                name: '',
                width: '4em',
                helpText: (
                  <div>
                    <em>Matching {this.props.displayName}</em> out of <em>Total {this.props.displayName}</em><br/>
                    with the given <em>{this.props.field.display}</em> value.
                  </div>
                ),
                renderCell: this.renderPrecentageCell
              }
            ]}
          >
          </Mesa>
        </div>
      </ModalBoundary>
    );
  }
}

MembershipField.propTypes = FieldFilter.propTypes

export default instrument(MembershipField)
