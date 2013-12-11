package model.modtree;

import static model.modtree.ModContext.ModContextType.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author Amineri
 */
public class ModTree {
	
	/**
	 * The tree's DocumentListener implementation.
	 */
	protected ModTreeListener mtListener = new ModTreeListener();
	
	/**
	 * The tree's list (queue) of pending Document Events to be processed.
	 */
	protected final List<DocumentEvent> docEvents;
	
	/**
	 * The tree's document it is listening to.
	 */
	protected Document doc = null;
	protected Document newDoc = null; // temp new doc for testing
	
	/**
	 * The tree's root node.
	 */
    private ModTreeRootNode currRootNode;
	private ModTreeRootNode prevRootNode;
	
	// TODO -- Should retrieve this information from ModDocument if stored there
	// @ XMTS : Would it make more sense to store these values here? Does the document even need them?
	/**
	 * The version number of the document.
	 */
	private int fileVersion = -1;

	/**
	 * The filename of the UPK file associated with this document.
	 */
	private String upkName = "";

	/**
	 * The GUID of the UPK file associated with this document.
	 */
	private String guid = "";

	/**
	 * The name of the targeted UnrealScript function.
	 */
	private String functionName = "";

	/**
	 * ModTree constructor.
	 * Initializes queue of 10 DocumentEvents.
	 * @param modDocument 
	 * @throws BadLocationException 
	 */
	public ModTree(Document document) throws BadLocationException {
		docEvents = new ArrayList<>();
		this.setDocument(document);
	}

	/**
	 * Associates a default document with the ModTree.
	 * Registers a DocumentListener with the document.
	 * @throws BadLocationException
	 */
	public ModTree() throws BadLocationException {
		this(new DefaultStyledDocument());
	}

	/**
	 * Associates a document with the ModTree.
	 * Registers a DocumentListener with the document.
	 * @param doc StyledDocument to be registered.
	 * @throws javax.swing.text.BadLocationException
	 */
	public void setDocument(Document doc) throws BadLocationException {
		this.doc = doc;
		if (doc.getLength() > 0) {
			String s = doc.getText(0, doc.getLength());
			ModTreeRootNode root = this.getRoot();
			root.insertString(0, s, null);
			root.reorganizeAfterInsertion();
			this.updateDocument();
		}
		doc.addDocumentListener(this.mtListener);
	}
	
	/**
	 * Retrieves current document associated with ModTree.
	 * @return
	 */
	public Document getDocument() {
		return doc;
	}
	
	/**
	 * Update the current document based on ModTree structure/content.
	 * Initially will only perform styling.
	 * Later may modify text to implement reference/offset corrections.
	 */
	protected void updateDocument() {
		if (getDocument() == null) {
			return;
		}
		int count = 0;
		int total = 0;
		List<Integer> updates = new ArrayList<>();
		if(this.prevRootNode == null || (this.currRootNode.getChildNodeCount() != this.prevRootNode.getChildCount())) {
			this.updateNodeStyles(this.currRootNode);
			System.out.println("ChildCount mismatch");
		} else {
			for (int i = 0; i < this.currRootNode.getChildNodeCount(); i++) {
				if(lineHasChanged(this.currRootNode.getChildNodeAt(i), this.prevRootNode.getChildNodeAt(i))) {
						this.updateNodeStyles(this.currRootNode.getChildNodeAt(i));
						count ++;
						updates.add(i+1);
				}
				total ++;
			}
			System.out.println(count + " lines out of " + total + " re-styled: " + updates);
		}
	}
	
	protected boolean lineHasChanged(ModTreeNode newLine, ModTreeNode oldLine) {
		if((oldLine.getContextFlag(HEX_CODE) || newLine.getContextFlag(HEX_CODE))
						&& newLine.getContextFlag(FILE_HEADER) != oldLine.getContextFlag(FILE_HEADER)) {
							return true;
		}
		if(true
						&& (newLine.isPlainText() == oldLine.isPlainText()) 
						&& newLine.getFullText().equals(oldLine.getFullText()) 
//						&& newLine.getContextFlag(HEX_CODE) == oldLine.getContextFlag(HEX_CODE) 
//						&& newLine.getContextFlag(VALID_CODE) == oldLine.getContextFlag(VALID_CODE) 
//						&& newLine.getContextFlag(FILE_HEADER) == oldLine.getContextFlag(FILE_HEADER) 
//						&& newLine.getContextFlag(AFTER_HEX) == oldLine.getContextFlag(AFTER_HEX) 
//						&& newLine.getContextFlag(BEFORE_HEX) == oldLine.getContextFlag(BEFORE_HEX) 
//						&& newLine.getContextFlag(HEX_HEADER) == oldLine.getContextFlag(HEX_HEADER)
				) {
							return false;
		}
//		System.out.println("Plaintext " + (newLine.isPlainText() == oldLine.isPlainText()) );
//		System.out.println("FullText " + (newLine.getFullText().equals(oldLine.getFullText())) );
//		System.out.println("HEX_CODE " + (newLine.getContextFlag(HEX_CODE) == newLine.getContextFlag(HEX_CODE)) );
//		System.out.println("VALID_CODE " + (newLine.getContextFlag(VALID_CODE) == newLine.getContextFlag(VALID_CODE)) );
//		System.out.println("FILE_HEADER " + (newLine.getContextFlag(FILE_HEADER) == newLine.getContextFlag(FILE_HEADER) ));
//		System.out.println("AFTER_HEX " + (newLine.getContextFlag(AFTER_HEX) == newLine.getContextFlag(AFTER_HEX)) );
//		System.out.println("BEFORE_HEX " + (newLine.getContextFlag(BEFORE_HEX) == newLine.getContextFlag(BEFORE_HEX)) );
//		System.out.println("HEX_HEADER " + (newLine.getContextFlag(HEX_HEADER) == newLine.getContextFlag(HEX_HEADER)));
		return true;
	}
	
	/**
	 * Updates associated document for a single node.
	 * Function is used recursively.
	 * @param node The current node being updated for.
	 */
	protected void updateNodeStyles(ModTreeNode node) {
		this.applyStyles(node);
		for (int i = 0; i < node.getChildNodeCount(); i++) {
			this.updateNodeStyles(node.getChildNodeAt(i));
		}
	}

	/**
	 * Applies any necessary styling for the current node to the document.
	 * WARNING : Do not make unnecessary text changes to the document.
	 * Attribute changes are ignored by ModTree.
	 * @param node The ModTreeNode originating the style. 
	 */
	protected void applyStyles(ModTreeNode node) {
		if(!node.isLeaf()) {
			return;
		}

		int start = node.getStartOffset();
		int end = node.getEndOffset();
		boolean replace = true;
		
		// perform attribute updates
		AttributeSet as = new SimpleAttributeSet(); // TODO perform node-to-style mapping
		StyleConstants.setForeground((MutableAttributeSet) as, Color.BLACK);
		StyleConstants.setItalic((MutableAttributeSet) as, false);
		if (node instanceof ModReferenceLeaf) {
			if (node.isVirtualFunctionRef()) {
				StyleConstants.setForeground((MutableAttributeSet) as, Color.MAGENTA);
				StyleConstants.setUnderline((MutableAttributeSet) as, true);
			} else {
				StyleConstants.setForeground((MutableAttributeSet) as, Color.ORANGE);
				StyleConstants.setUnderline((MutableAttributeSet) as, true);
			}
		}
		if ((node.getContextFlag(HEX_CODE) && !node.getContextFlag(VALID_CODE)) 
				) {
			StyleConstants.setForeground((MutableAttributeSet) as, new Color(255, 128, 128));
			StyleConstants.setStrikeThrough((MutableAttributeSet) as, replace);
		}
		if (node.getName().equals("OperandToken")) {
			if (node.getFullText().startsWith("0B")) {
				StyleConstants.setForeground((MutableAttributeSet) as, Color.DARK_GRAY);
				StyleConstants.setBold((MutableAttributeSet) as, false);
			} else {
				StyleConstants.setForeground((MutableAttributeSet) as, Color.BLUE);
				StyleConstants.setBold((MutableAttributeSet) as, true);
			}
		}
		if (node.getName().contains("Jump")) {
			StyleConstants.setBackground((MutableAttributeSet) as,
					new Color( 255, 255, 128));
		}
//		((StyledDocument) newDoc).setCharacterAttributes( start, end, as, replace);
		((StyledDocument) this.getDocument()).setCharacterAttributes(
				start, end-start, as, replace);
//		System.out.println("Applied Style from : " + start + " to " + end);
	}
	
	/**
	 * Processes next DocumentEvent in the queue.
	 * Updates the ModTree model, then updates the registered document
	 * TODO -- Event/thread trigger on this when docEvents not empty
	 * @throws BadLocationException 
	 */
	public void processNextEvent() throws BadLocationException {
		if (!docEvents.isEmpty()) {
			System.out.print("Starting processing Document Event... \n");
			long startTime = System.currentTimeMillis();
			this.processDocumentEvent(docEvents.get(0));
			System.out.print("Document Event processing done, took " + (System.currentTimeMillis() - startTime) + "ms\n");
		}
	}
	
	/**
	 * Processes a single specified DocumentEvent.
	 * Inserts or removes text and then reorganizes the tree.
	 * @param de The DocumentEvent
	 * @throws BadLocationException
	 */
	protected void processDocumentEvent(DocumentEvent de) throws BadLocationException {
		if (de == null) {
			return;
		}
//		int offset = de.getOffset();
//		int length = de.getLength();
//		String s = de.getDocument().getText(offset, length);
//		ModTreeRootNode r = this.getRoot();
//		r.initUpdateFlags(false);
//		EventType type = de.getType();
//		if (type == EventType.INSERT) {
//			r.insertString(offset, s, null);
//			r.reorganizeAfterInsertion();
//		} else if (type == EventType.REMOVE) {
//			r.remove(offset, length);
//			r.reorganizeAfterDeletion();
//		}
		if (de.getDocument().getLength() > 0) {
			String s = de.getDocument().getText(0, doc.getLength());
			this.prevRootNode = currRootNode;
			this.currRootNode = new ModTreeRootNode(this);
			System.out.print("Inserting text... ");
			long startTime = System.currentTimeMillis();
			this.currRootNode.insertString(0, s, null);
			System.out.print(" done, took " + (System.currentTimeMillis() - startTime) + "ms\n");
			System.out.print("Parsing text... ");
			startTime = System.currentTimeMillis();
			this.currRootNode.reorganizeAfterInsertion();
			System.out.print(" done, took " + (System.currentTimeMillis() - startTime) + "ms\n");
//			this.updateDocument();
		}
		docEvents.clear();
//		docEvents.remove(de);
		if (this.getDocument() != null && docEvents.isEmpty()) {
			System.out.print("Styling document ... ");
			long startTime = System.currentTimeMillis();
			this.updateDocument();
			System.out.print(" done, took " + (System.currentTimeMillis() - startTime) + "ms\n");
		}
	}

	/**
	 * Returns the root node of the document.
	 * @return the root node
	 */
	public ModTreeRootNode getRoot() {
		if (this.currRootNode == null) {
			// lazily instantiate root node
			this.currRootNode = new ModTreeRootNode(this);
		}
		return this.currRootNode;
	}

	// TODO: redirect these calls to the Document if necessary
	/**
	 * Returns the file version number.
	 * @return the version number
	 */
	public int getFileVersion() {
		return this.fileVersion;
	}

	/**
	 * Sets the file version number
	 * @param fileVersion the version number to set.
	 */
	public void setFileVersion(int fileVersion) {
		this.fileVersion = fileVersion;
	}

	/**
	 * Returns the filename of the UPK file associated with this document.
	 * @return the UPK filename
	 */
	public String getUpkName() {
		return this.upkName;
	}

	/**
	 * Sets the filename of the UPK file associated with this document.
	 * @param upkName the filename to set
	 */
	public void setUpkName(String upkName) {
		this.upkName = upkName;
	}

	/**
	 * Returns the GUID of the UPK file associated with this document.
	 * @return the GUID
	 */
	public String getGuid() {
		return this.guid;
	}

	/**
	 * Sets the GUID of the UPK file associated with this document.
	 * @param guid the GUID to set
	 */
	public void setGuid(String guid) {
		this.guid = guid;
	}

	/**
	 * Returns the name of the targeted UnrealScript function.
	 * @return the function name
	 */
	public String getFunctionName() {
		return this.functionName;
	}

	/**
	 * Sets the name of the targeted UnrealScript function.
	 * @param functionName the function name
	 */
	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	/**
	 * Implements the Listener to be registered with a StyledDocument
	 */
	private class ModTreeListener implements DocumentListener {

		@Override
		public void insertUpdate(DocumentEvent evt) {
			this.update(evt);
		}

		@Override
		public void removeUpdate(DocumentEvent evt) {
			this.update(evt);
		}

		@Override
		public void changedUpdate(DocumentEvent evt) {
//			System.out.println("new event: " + evt.getType());
			// do nothing for formatting changes
		}
		
		private void update(DocumentEvent evt) {
			System.out.println("new event: " + evt.getType());
			docEvents.add(evt);
//			new SwingWorker<Object, Object>() {
//
//				@Override
//				protected Object doInBackground() throws Exception {
//					while (!docEvents.isEmpty()) {
//						ModTree.this.processNextEvent();
//					}
//					return null;
//				}
//				
//				@Override
//				protected void done() {
//					System.out.println("done");
//				};
//
//			}.execute();
//			SwingUtilities.invokeLater(new Runnable() {
//				
//				@Override
//				public void run() {
//					while (!docEvents.isEmpty()) {
//						try {
//							ModTree.this.processNextEvent();
//						} catch (BadLocationException e) {
//							e.printStackTrace();
//						}
//					}
//					System.out.println("done");
//				}
//			});
			new Thread() {
				public void run() {
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								while (!docEvents.isEmpty()) {
									try {
										ModTree.this.processNextEvent();
									} catch (BadLocationException e) {
										e.printStackTrace();
									}
								}
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("done");
				};
		}.start();
		}
	}

}