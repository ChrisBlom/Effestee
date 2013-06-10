package blom.effestee;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import blom.effestee.semiring.Pair;

public class TreeNode<S> implements Iterable<List<S>> {

	List<TreeNode<S>> children = new ArrayList<>(0);
	S value;

	TreeNode<S> parent;
	private boolean isAccept;

	private TreeNode(TreeNode<S> parent, S value) {
		this.parent = parent;
		this.value = value;
	}

	public TreeNode<S> addChild(S transitionIndex) {
		TreeNode<S> child = new TreeNode<>(this, transitionIndex);
		this.children.add(child);
		return child;
	}

	public static <S> TreeNode<S> create() {
		return new TreeNode<S>(null, null);
	}

	public void markAccept() {
		this.isAccept = true;

	}

	@Override
	public Iterator<List<S>> iterator() {

		TreeNode<S> rootIntermediate = this;
		while (this.parent != null) {
			rootIntermediate = this.parent;
		}

		final ArrayDeque<TreeNode<S>> stack = new ArrayDeque<TreeNode<S>>();
		stack.add(rootIntermediate);
		
		List<List<S>> paths = new ArrayList<>();
		
		while( !stack.isEmpty()) {
			TreeNode<S> node = seekAccept(stack);
			if( node == null) {
				break;
			}
			paths.add( node.getPath() );

		};
		
		return paths.iterator();

	}

	protected List<S> getPath() {

		ArrayDeque<S> path = new ArrayDeque<S>();

		TreeNode<S> current = this;
		while (current.parent != null) {
			path.addFirst(current.value);
			current = current.parent;
		}

		return new ArrayList<>(path);
	}

	private static <S> TreeNode<S> seekAccept(ArrayDeque<TreeNode<S>> stack) {
		while (!stack.isEmpty()) {
			TreeNode<S> top = stack.pop();
			stack.addAll(top.children);
			if (top.isAccept()) {
				return top;
			}
		}
		return null;
	}

	private boolean isAccept() {
		return this.isAccept;
	}
}
