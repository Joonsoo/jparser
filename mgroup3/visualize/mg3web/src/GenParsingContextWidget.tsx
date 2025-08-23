import cytoscape from 'cytoscape';
import dagre from 'cytoscape-dagre';
import {useEffect, useRef} from "react";
import {GenNodeId, GenParsingContextGraph} from "@/GenParsingContext";

cytoscape.use(dagre);

export function GenParsingContextGraphWidget({graph}: { graph: GenParsingContextGraph }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    console.log("GenParsingContextGraphWidget", ref);
    if (ref.current !== null) {
      let cy = cytoscape({
        container: ref.current, // document.getElementById('cy'),

        boxSelectionEnabled: false,
        autounselectify: true,

        layout: {
          name: 'dagre'
        },

        style: [
          {
            selector: 'node',
            style: {
              'background-color': '#11479e',
              'label': 'data(id)',
            }
          },
          {
            selector: 'edge',
            style: {
              'width': 4,
              'target-arrow-shape': 'triangle',
              'line-color': '#9dbaea',
              'target-arrow-color': '#9dbaea',
              'curve-style': 'bezier'
            }
          }
        ],

        elements: {
          nodes: graph.nodes.map((node) => {
            return {
              data: {
                id: GenNodeId(node),
                label: `${node.symbolId}_${node.pointer}_${node.startGen}_${node.endGen}`,
              },
              classes: 'gennode',
            };
          }),
          edges: graph.edges.map((edge) => {
            return {
              data: {source: GenNodeId(edge.first), target: GenNodeId(edge.second)}
            };
          }),
        }
      });
    }
  }, [ref]);

  return (<div ref={ref} style={{position: 'absolute', width: '100%', height: '100%'}}></div>)
}
