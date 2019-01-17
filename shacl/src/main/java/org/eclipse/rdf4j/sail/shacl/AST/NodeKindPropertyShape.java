/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;


import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleLiteral;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.MinLengthFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.NodeKindFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 */
public class NodeKindPropertyShape extends PathPropertyShape {

	private final NodeKind nodeKind;
	private static final Logger logger = LoggerFactory.getLogger(NodeKindPropertyShape.class);

	NodeKindPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape) {
		super(id, connection, nodeShape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.NODE_KIND_PROP, null, true))) {
			nodeKind = stream.map(Statement::getObject).map(v -> (Resource) v).map(NodeKind::from).findAny().orElseThrow(() -> new RuntimeException("Expected to find sh:nodeKind on " + id));
		}

	}

	public enum NodeKind{

		BlankNode(SHACL.BLANK_NODE),
		IRI(SHACL.IRI),
		Literal(SHACL.LITERAL),
		BlankNodeOrIRI(SHACL.BLANK_NODE_OR_IRI),
		BlankNodeOrLiteral(SHACL.BLANK_NODE_OR_LITERAL),
		IRIOrLiteral(SHACL.IRI_OR_LITERAL),
		;

		IRI iri;
		NodeKind(IRI iri) {
			this.iri = iri;
		}

		public static NodeKind from(Resource resource){
			for (NodeKind value : NodeKind.values()) {
				if(value.iri.equals(resource)) return value;
			}

			throw new IllegalStateException("Unknown nodeKind: "+resource);
		}
	}



	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, boolean assumeBaseSailValid) {

		PlanNode invalidValues =  StandardisedPlanHelper.getGenericSingleObjectPlan(
			shaclSailConnection,
			nodeShape,
			(parent, trueNode, falseNode) -> new NodeKindFilter(parent, trueNode, falseNode, nodeKind),
			this
		);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return invalidValues;

	}

	@Override
	public boolean requiresEvaluation(Repository addedStatements, Repository removedStatements) {
		return true;
	}
}
