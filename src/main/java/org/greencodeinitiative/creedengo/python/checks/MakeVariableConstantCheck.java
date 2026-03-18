/*
 * creedengo - Python language - Provides rules to reduce the environmental footprint of your Python programs
 * Copyright © 2024 Green Code Initiative (https://green-code-initiative.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.greencodeinitiative.creedengo.python.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.AssignmentStatement;
import org.sonar.plugins.python.api.tree.CompoundAssignmentStatement;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.Tree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Rule(key = "GCI82")
public class MakeVariableConstantCheck extends PythonSubscriptionCheck {

    private static final String MESSAGE = "Make this variable a constant by renaming it to uppercase";

    private static final class VarInfo {
        final Tree firstNode;
        int writes;

        VarInfo(Tree firstNode) {
            this.firstNode = firstNode;
            this.writes = 1;
        }
    }

    @Override
    public void initialize(Context context) {
        context.registerSyntaxNodeConsumer(Tree.Kind.FILE_INPUT, this::analyzeFile);
    }

    private void analyzeFile(SubscriptionContext context) {
        Tree root = context.syntaxNode();

        Map<String, VarInfo> vars = new HashMap<>();
        Set<String> invalid = new HashSet<>();

        scanTree(root, vars, invalid);

        for (VarInfo info : vars.values()) {
            context.addIssue(info.firstNode.firstToken(), MESSAGE);
        }
    }

    // Recursively scans the AST to detect assignments and compound assignments
    private void scanTree(Tree node, Map<String, VarInfo> vars, Set<String> invalid) {
        if (node.is(Tree.Kind.ASSIGNMENT_STMT)) {
            handleAssignment((AssignmentStatement) node, vars, invalid);
        } else if (node.is(Tree.Kind.COMPOUND_ASSIGNMENT)) {
            handleCompound((CompoundAssignmentStatement) node, vars, invalid);
        }

        for (Tree child : node.children()) {
            scanTree(child, vars, invalid);
        }
    }

    //Handles standard assignements (x = 10)
    private void handleAssignment(AssignmentStatement stmt, Map<String, VarInfo> vars, Set<String> invalid) {
        if (stmt.lhsExpressions().size() != 1) {
            return;
        }
        if (stmt.lhsExpressions().get(0).expressions().isEmpty()) {
            return;
        }

        Expression lhs = stmt.lhsExpressions().get(0).expressions().get(0);
        if (!lhs.is(Tree.Kind.NAME)) {
            return;
        }

        String name = ((Name) lhs).name();

        if (!isConstantCandidate(name)) {
            invalidate(name, vars, invalid);
            return;
        }
        
        if (invalid.contains(name)) {
            return;
        }

        VarInfo info = vars.get(name);
        if (info == null) {
            vars.put(name, new VarInfo(lhs));
        } else {
            info.writes++;
            if (info.writes > 1) {
                invalidate(name, vars, invalid);
            }
        }
    }

    // Handles compound assignments (x += 1)
    private void handleCompound(CompoundAssignmentStatement stmt, Map<String, VarInfo> vars, Set<String> invalid) {
        Expression lhs = stmt.lhsExpression();
        if (!lhs.is(Tree.Kind.NAME)) {
            return;
        }

        String name = ((Name) lhs).name();
        invalidate(name, vars, invalid);
    }

    // Removes a variable from constant candidates and marks it as invalid
    private void invalidate(String name, Map<String, VarInfo> vars, Set<String> invalid) {
        invalid.add(name);
        vars.remove(name);
    }

    // Checks if the variable name is a candidate for being made constant (not starting with _ and not already uppercase)
    private boolean isConstantCandidate(String name) {
        if (name == null || name.isEmpty()) { 
            return false;
        }

        if (name.startsWith("_")) {
            return false;
        }

        return !isUpperCaseName(name);
    }

    // Checks if the name is already in uppercase (and thus not a candidate for being made constant)
    private boolean isUpperCaseName(String name) {
        for (char c : name.toCharArray()) {
            if (!((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')) {
                return false;
            }
        }
        return true;
    }
}