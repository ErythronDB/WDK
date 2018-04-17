'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.UiStateDefaults = exports.OptionsDefaults = exports.ColumnDefaults = undefined;

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _Icon = require('./Components/Icon');

var _Icon2 = _interopRequireDefault(_Icon);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var ColumnDefaults = exports.ColumnDefaults = {
  primary: false,
  searchable: true,
  sortable: true,
  resizeable: true,
  truncated: false,

  filterable: false,
  filterState: {
    enabled: false,
    visible: false,
    blacklist: []
  },

  hideable: true,
  hidden: false,

  disabled: false,
  type: 'text'
};

var OptionsDefaults = exports.OptionsDefaults = {
  title: null,
  toolbar: true,
  inline: false,
  className: null,
  showCount: true,
  errOnOverflow: false,
  editableColumns: true,
  overflowHeight: '16em',
  searchPlaceholder: 'Search This Table',
  isRowSelected: function isRowSelected(row, index) {
    return false;
  }
};

var UiStateDefaults = exports.UiStateDefaults = {
  searchQuery: null,
  filteredRowCount: 0,
  sort: {
    columnKey: null,
    direction: 'asc'
  },
  pagination: {
    currentPage: 1,
    totalPages: null,
    totalRows: null,
    rowsPerPage: 20
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9EZWZhdWx0cy5qc3giXSwibmFtZXMiOlsiQ29sdW1uRGVmYXVsdHMiLCJwcmltYXJ5Iiwic2VhcmNoYWJsZSIsInNvcnRhYmxlIiwicmVzaXplYWJsZSIsInRydW5jYXRlZCIsImZpbHRlcmFibGUiLCJmaWx0ZXJTdGF0ZSIsImVuYWJsZWQiLCJ2aXNpYmxlIiwiYmxhY2tsaXN0IiwiaGlkZWFibGUiLCJoaWRkZW4iLCJkaXNhYmxlZCIsInR5cGUiLCJPcHRpb25zRGVmYXVsdHMiLCJ0aXRsZSIsInRvb2xiYXIiLCJpbmxpbmUiLCJjbGFzc05hbWUiLCJzaG93Q291bnQiLCJlcnJPbk92ZXJmbG93IiwiZWRpdGFibGVDb2x1bW5zIiwib3ZlcmZsb3dIZWlnaHQiLCJzZWFyY2hQbGFjZWhvbGRlciIsImlzUm93U2VsZWN0ZWQiLCJyb3ciLCJpbmRleCIsIlVpU3RhdGVEZWZhdWx0cyIsInNlYXJjaFF1ZXJ5IiwiZmlsdGVyZWRSb3dDb3VudCIsInNvcnQiLCJjb2x1bW5LZXkiLCJkaXJlY3Rpb24iLCJwYWdpbmF0aW9uIiwiY3VycmVudFBhZ2UiLCJ0b3RhbFBhZ2VzIiwidG90YWxSb3dzIiwicm93c1BlclBhZ2UiXSwibWFwcGluZ3MiOiI7Ozs7Ozs7QUFBQTs7OztBQUVBOzs7Ozs7QUFFTyxJQUFNQSwwQ0FBaUI7QUFDNUJDLFdBQVMsS0FEbUI7QUFFNUJDLGNBQVksSUFGZ0I7QUFHNUJDLFlBQVUsSUFIa0I7QUFJNUJDLGNBQVksSUFKZ0I7QUFLNUJDLGFBQVcsS0FMaUI7O0FBTzVCQyxjQUFZLEtBUGdCO0FBUTVCQyxlQUFhO0FBQ1hDLGFBQVMsS0FERTtBQUVYQyxhQUFTLEtBRkU7QUFHWEMsZUFBVztBQUhBLEdBUmU7O0FBYzVCQyxZQUFVLElBZGtCO0FBZTVCQyxVQUFRLEtBZm9COztBQWlCNUJDLFlBQVUsS0FqQmtCO0FBa0I1QkMsUUFBTTtBQWxCc0IsQ0FBdkI7O0FBcUJBLElBQU1DLDRDQUFrQjtBQUM3QkMsU0FBTyxJQURzQjtBQUU3QkMsV0FBUyxJQUZvQjtBQUc3QkMsVUFBUSxLQUhxQjtBQUk3QkMsYUFBVyxJQUprQjtBQUs3QkMsYUFBVyxJQUxrQjtBQU03QkMsaUJBQWUsS0FOYztBQU83QkMsbUJBQWlCLElBUFk7QUFRN0JDLGtCQUFnQixNQVJhO0FBUzdCQyxxQkFBbUIsbUJBVFU7QUFVN0JDLGlCQUFlLHVCQUFDQyxHQUFELEVBQU1DLEtBQU4sRUFBZ0I7QUFDN0IsV0FBTyxLQUFQO0FBQ0Q7QUFaNEIsQ0FBeEI7O0FBZUEsSUFBTUMsNENBQWtCO0FBQzdCQyxlQUFhLElBRGdCO0FBRTdCQyxvQkFBa0IsQ0FGVztBQUc3QkMsUUFBTTtBQUNKQyxlQUFXLElBRFA7QUFFSkMsZUFBVztBQUZQLEdBSHVCO0FBTzdCQyxjQUFZO0FBQ1ZDLGlCQUFhLENBREg7QUFFVkMsZ0JBQVksSUFGRjtBQUdWQyxlQUFXLElBSEQ7QUFJVkMsaUJBQWE7QUFKSDtBQVBpQixDQUF4QiIsImZpbGUiOiJEZWZhdWx0cy5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBSZWFjdCBmcm9tICdyZWFjdCc7XG5cbmltcG9ydCBJY29uIGZyb20gJy4vQ29tcG9uZW50cy9JY29uJztcblxuZXhwb3J0IGNvbnN0IENvbHVtbkRlZmF1bHRzID0ge1xuICBwcmltYXJ5OiBmYWxzZSxcbiAgc2VhcmNoYWJsZTogdHJ1ZSxcbiAgc29ydGFibGU6IHRydWUsXG4gIHJlc2l6ZWFibGU6IHRydWUsXG4gIHRydW5jYXRlZDogZmFsc2UsXG5cbiAgZmlsdGVyYWJsZTogZmFsc2UsXG4gIGZpbHRlclN0YXRlOiB7XG4gICAgZW5hYmxlZDogZmFsc2UsXG4gICAgdmlzaWJsZTogZmFsc2UsXG4gICAgYmxhY2tsaXN0OiBbXVxuICB9LFxuXG4gIGhpZGVhYmxlOiB0cnVlLFxuICBoaWRkZW46IGZhbHNlLFxuXG4gIGRpc2FibGVkOiBmYWxzZSxcbiAgdHlwZTogJ3RleHQnXG59O1xuXG5leHBvcnQgY29uc3QgT3B0aW9uc0RlZmF1bHRzID0ge1xuICB0aXRsZTogbnVsbCxcbiAgdG9vbGJhcjogdHJ1ZSxcbiAgaW5saW5lOiBmYWxzZSxcbiAgY2xhc3NOYW1lOiBudWxsLFxuICBzaG93Q291bnQ6IHRydWUsXG4gIGVyck9uT3ZlcmZsb3c6IGZhbHNlLFxuICBlZGl0YWJsZUNvbHVtbnM6IHRydWUsXG4gIG92ZXJmbG93SGVpZ2h0OiAnMTZlbScsXG4gIHNlYXJjaFBsYWNlaG9sZGVyOiAnU2VhcmNoIFRoaXMgVGFibGUnLFxuICBpc1Jvd1NlbGVjdGVkOiAocm93LCBpbmRleCkgPT4ge1xuICAgIHJldHVybiBmYWxzZTtcbiAgfVxufTtcblxuZXhwb3J0IGNvbnN0IFVpU3RhdGVEZWZhdWx0cyA9IHtcbiAgc2VhcmNoUXVlcnk6IG51bGwsXG4gIGZpbHRlcmVkUm93Q291bnQ6IDAsXG4gIHNvcnQ6IHtcbiAgICBjb2x1bW5LZXk6IG51bGwsXG4gICAgZGlyZWN0aW9uOiAnYXNjJ1xuICB9LFxuICBwYWdpbmF0aW9uOiB7XG4gICAgY3VycmVudFBhZ2U6IDEsXG4gICAgdG90YWxQYWdlczogbnVsbCxcbiAgICB0b3RhbFJvd3M6IG51bGwsXG4gICAgcm93c1BlclBhZ2U6IDIwXG4gIH1cbn07XG4iXX0=