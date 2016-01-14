/**
 * Get a sub-tree from an Ontology. The `leafPredicate` function
 * is used to find the leaves of the tree to return.
 *
 * @param {Ontology} ontology
 * @param {Function} leafPredicate
 */
export function getTree(ontology, leafPredicate) {
  return compactRootNodes(pruneTreeByLeaves(ontology.tree, leafPredicate));
}

/**
 * Given a node `root`, returns a new node such that all leaves pass
 * `leafPredicate`. If no descendant of `root` passes, and if `root` does not
 * pass, then `undefined` will be returned.
 */
export function pruneTreeByLeaves(root, leafPredicate) {
  let clonedRoot = Object.assign({}, root, {
    children: (root.children || []).map(c => pruneTreeByLeaves(c, leafPredicate)).filter(c => c != null)
  })

  // If any children match the leaf predicate, we will return the clonedRoot.
  // Or, if the clonedRoot matched the leafPredicate, we will return the clonedRoot.
  if (clonedRoot.children.length > 0 || leafPredicate(clonedRoot)) {
    return clonedRoot;
  }
}

/**
 * If the root node has only one child, and that child only has one child,
 * replace the root node with it's child.
 *
 */
export function compactRootNodes(root) {
  return root.children.length === 1 && root.children[0].children.length === 1
    ? compactRootNodes(root.children[0])
    : root;
}