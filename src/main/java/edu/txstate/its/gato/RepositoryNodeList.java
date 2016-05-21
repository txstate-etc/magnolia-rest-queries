package edu.txstate.its.gato;

import info.magnolia.rest.service.node.v1.RepositoryNode;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a list of nodes returned from a JCR query.
 */
@XmlRootElement(name="nodes")
class RepositoryNodeList extends AbstractList<RepositoryNode> {

  @XmlElement(name="node")
  protected final List<RepositoryNode> nodes = new ArrayList<RepositoryNode>();

  @Override
  public RepositoryNode get(int index) {
    return nodes.get(index);
  }

  @Override
  public boolean add(RepositoryNode n) {
    return nodes.add(n);
  }

  @Override
  public int size() {
    return nodes.size();
  }
}
