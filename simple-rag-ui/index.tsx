import React from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './src/App';

// Standard web entry: mount React app into a root element.
const ensureRoot = (): HTMLElement => {
	let root = document.getElementById('root');
	if (!root) {
		root = document.createElement('div');
		root.id = 'root';
		document.body.appendChild(root);
	}
	return root;
};

const container = ensureRoot();
createRoot(container).render(React.createElement(App));
