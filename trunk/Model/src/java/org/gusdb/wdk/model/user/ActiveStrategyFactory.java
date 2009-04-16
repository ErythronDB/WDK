/**
 * 
 */
package org.gusdb.wdk.model.user;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.json.JSONException;

/**
 * @author xingao
 * 
 */
class ActiveStrategyFactory {

    /**
     * newly opened strategies is always at the end, which means it has the
     * largest order number, and will be displayed first.
     */
    private ActiveStrategy root;
    private User user;

    /**
     * 
     */
    ActiveStrategyFactory(User user) {
        this.user = user;
        root = new ActiveStrategy(null);
    }

    /**
     * The method returns an array of top strategies; nested strategies are
     * computed later.
     * 
     * @return
     * @throws SQLException
     * @throws JSONException
     * @throws WdkModelException
     * @throws WdkUserException
     */
    int[] getRootStrategies() {
        int[] ids = new int[root.children.size()];
        int i = 0;
        for (ActiveStrategy strategy : root.children.values()) {
            ids[i++] = strategy.strategyId;
        }
        return ids;
    }

    synchronized void openActiveStrategy(String strategyKey)
            throws NumberFormatException, WdkUserException, WdkModelException,
            JSONException, SQLException {
        if (getStrategy(strategyKey) != null) return;

        String parentKey = getParentKey(strategyKey);
        ActiveStrategy parent = getStrategy(parentKey);
        if (!parent.children.containsKey(strategyKey)) {
            ActiveStrategy strategy = new ActiveStrategy(strategyKey);
            strategy.parent = parent;
            parent.children.put(strategyKey, strategy);
        }
    }

    synchronized void closeActiveStrategy(String strategyKey) {
        ActiveStrategy strategy = getStrategy(strategyKey);
        if (strategy == null) return;
        strategy.parent.children.remove(strategyKey);
    }

    /**
     * @param strategyKeys
     *            the strategies in the array should all share the same parents;
     *            that is, they should siblings.
     * @throws WdkUserException
     */
    synchronized void orderActiveStrategies(String[] strategyKeys)
            throws WdkUserException {
        if (strategyKeys.length == 0) return;
        ActiveStrategy strategy = getStrategy(strategyKeys[0]);
        if (strategy == null || strategy.strategyKey == null)
            throw new WdkUserException("The strategy '" + strategyKeys[0]
                    + "' is not opened!");
        LinkedHashMap<String, ActiveStrategy> map = new LinkedHashMap<String, ActiveStrategy>();
        map.put(strategy.strategyKey, strategy);
        for (int i = 1; i < strategyKeys.length; i++) {
            ActiveStrategy sibling = getStrategy(strategyKeys[i]);
            if (sibling == null)
                throw new WdkUserException("The strategy '" + strategyKeys[i]
                        + "' is not opened!");
            if (!strategy.parent.equals(sibling.parent))
                throw new WdkUserException("the two strategies '"
                        + strategyKeys[0] + "' and '" + strategyKeys[i]
                        + "' are not under that same parent.");
            map.put(sibling.strategyKey, sibling);
        }
        strategy.parent.children.clear();
        strategy.parent.children.putAll(map);
    }

    synchronized void replaceStrategy(User user, int oldId, int newId,
            Map<Integer, Integer> stepMap) throws WdkUserException,
            WdkModelException, JSONException, SQLException {
        ActiveStrategy oldStrategy = root.children.get(Integer.toString(oldId));
        ActiveStrategy newStrategy = new ActiveStrategy(Integer.toString(newId));
        newStrategy.parent = root;
        if (stepMap == null) stepMap = new LinkedHashMap<Integer, Integer>();
        Strategy strategy = user.getStrategy(newId);
        replaceStrategy(strategy, oldStrategy, newStrategy, stepMap);

        LinkedHashMap<String, ActiveStrategy> children = new LinkedHashMap<String, ActiveStrategy>();
        for (String strategyKey : root.children.keySet()) {
            // use new strategy to replace the old one at the same place
            if (oldStrategy.strategyKey.equals(strategyKey)) {
                children.put(newStrategy.strategyKey, newStrategy);
            } else {
                children.put(strategyKey, root.children.get(strategyKey));
            }
        }
        root.children = children;
    }

    int getOrder(String strategyKey) {
        ActiveStrategy strategy = getStrategy(strategyKey);
        if (strategy == null || strategy.strategyKey == null) return 0;
        int order = 1;
        System.out.println("current: " + strategyKey + ", parent: "
                + strategy.parent.strategyKey + ", children: "
                + strategy.parent.children.size());
        for (ActiveStrategy sibling : strategy.parent.children.values()) {
            if (strategy.equals(sibling)) return order;
            order++;
        }
        return 0;
    }

    synchronized void clear() {
        root.children.clear();
    }

    private String getParentKey(String strategyKey) throws WdkUserException,
            WdkModelException, JSONException, SQLException {
        int pos = strategyKey.indexOf('_');
        if (pos < 0) return null;
        int strategyId = Integer.parseInt(strategyKey.substring(0, pos));
        int stepId = Integer.parseInt(strategyKey.substring(pos + 1));
        Strategy strategy = user.getStrategy(strategyId);
        Step parent = strategy.getStepById(stepId).getParentStep();
        while (parent.getNextStep() != null) {
            parent = parent.getNextStep();
        }
        // check if the parent is top level
        if (parent.getParentStep() == null) return Integer.toString(strategyId);
        else return strategyId + "_" + parent.getDisplayId();
    }

    private ActiveStrategy getStrategy(String strategyKey) {
        if (strategyKey == null) return root;
        else return root.getDescendent(strategyKey);
    }

    private void replaceStrategy(Strategy strategy, ActiveStrategy oldStrategy,
            ActiveStrategy newStrategy, Map<Integer, Integer> stepMap) {
        System.out.println("replace old: " + oldStrategy.strategyKey
                + ", new: " + newStrategy.strategyKey);
        for (int old : stepMap.keySet()) {
            System.out.println("step " + old + "->" + stepMap.get(old));
        }
        for (ActiveStrategy oldChild : oldStrategy.children.values()) {
            String oldKey = oldChild.strategyKey;
            int oldId = Integer.parseInt(oldKey.substring(oldKey.indexOf('_') + 1));
            Integer newId = stepMap.get(oldId);
            if (newId == null) {
                Step step;
                try {
                    step = strategy.getStepById(oldId);
                    if (step == null) throw new WdkModelException();
                    newId = oldId;
                } catch (Exception ex) { // step no longer exist
                    continue; // skip this branch
                }
            }
            String newKey = newStrategy.strategyId + "_" + newId;
            ActiveStrategy newChild = new ActiveStrategy(newKey);
            newChild.parent = newStrategy;
            replaceStrategy(strategy, oldChild, newChild, stepMap);
            newStrategy.children.put(newKey, newChild);
        }
    }
}
