package widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Stack;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyleConstants;

import chat.Vocabulary;
import examples.widgets.ListExampleFrame;
//import examples.widgets.ListExampleFrame.AddAction;
//import examples.widgets.ListExampleFrame.ClearSelectionAction;
import examples.widgets.ListExampleFrame.ColorTextRenderer;
//import examples.widgets.ListExampleFrame.RemoveItemAction;
import models.Message;

/**
 * Fenêtre d'affichae de la version GUI texte du client de chat.
 * @author davidroussel
 */
public class ClientFrame2 extends AbstractClientFrame
{
	private static String newline = System.getProperty("line.separator");
	/**
	 * Lecteur de flux d'entrée. Lit les données texte du {@link #inPipe} pour
	 * les afficher dans le {@link #document}
	 */
	private ObjectInputStream inBR;

	/**
	 * Le label indiquant sur quel serveur on est connecté
	 */
	protected final JLabel serverLabel;

	/**
	 * La zone du texte à envoyer
	 */
	protected final JTextField sendTextField;

	/**
	 * Actions à réaliser lorsque l'on veut effacer le contenu du document
	 */
	private final ClearAction clearAction;

	/**
	 * Actions à réaliser lorsque l'on veut envoyer un message au serveur
	 */
	private final SendAction sendAction;

	/**
	 * Actions à réaliser lorsque l'on veut envoyer un message au serveur
	 */
	protected final QuitAction quitAction;

	/**
	 * Référence à la fenêtre courante (à utiliser dans les classes internes)
	 */
	protected final JFrame thisRef;

	private DefaultListModel<String> elements = new DefaultListModel<String>();

	/**
	 * Le modèle de sélection de la JList.
	 * Conserve les indices des éléments sélectionnés de {@link #elements} dans
	 * la JList qui affiche ces éléments.
	 */
	private ListSelectionModel selectionModel = null;

	/**
	 * La text area où afficher les messages
	 */
	private JTextArea output = null;

	/**
	 * Action à réaliser lorsque l'on souhaite supprimer les éléments
	 * sélectionnnés de la liste
	 */
	private final Action removeAction = new RemoveItemAction();

	/**
	 * Action à réaliser lorsque l'on souhaite déselctionner tous les élements de la liste
	 */
	private final Action clearSelectionAction = new ClearSelectionAction();

	/**
	 * Action à réaliser lorsque l'on souhaite ajouter un élément à la liste
	 */
	private final Action addAction = new AddAction();
	
	
	/**
	 * Constructeur de la fenêtre
	 * @param name le nom de l'utilisateur
	 * @param host l'hôte sur lequel on est connecté
	 * @param commonRun état d'exécution des autres threads du client
	 * @param parentLogger le logger parent pour les messages
	 * @throws HeadlessException
	 */
	public ClientFrame2(String name,
	                   String host,
	                   Boolean commonRun,
	                   Logger parentLogger)
	    throws HeadlessException
	{
		super(name, host, commonRun, parentLogger);
		thisRef = this;

		// --------------------------------------------------------------------
		// Flux d'IO
		//---------------------------------------------------------------------
		/*
		 * Attention, la création du flux d'entrée doit (éventuellement) être
		 * reportée jusqu'au lancement du run dans la mesure où le inPipe
		 * peut ne pas encore être connecté à un PipedOutputStream
		 */

		// --------------------------------------------------------------------
		// Création des actions send, clear et quit
		// --------------------------------------------------------------------

		sendAction = new SendAction();
		clearAction = new ClearAction();
		quitAction = new QuitAction();


		/*
		 * Ajout d'un listener pour fermer correctement l'application lorsque
		 * l'on ferme la fenêtre. WindowListener sur this
		 */
		addWindowListener(new FrameWindowListener());

		// --------------------------------------------------------------------
		// Widgets setup (handled by Window builder)
		// --------------------------------------------------------------------

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		getContentPane().add(toolBar, BorderLayout.NORTH);

		//TODO button
		
		JButton quitButton = new JButton(quitAction);
		toolBar.add(quitButton);
		
		
		JButton btnClearSelection = new JButton(clearSelectionAction);
		toolBar.add(btnClearSelection);
		
		JButton	btnKick = new JButton("");
		btnKick.setAction(removeAction);
		toolBar.add(btnKick);
		
		JButton clearButton = new JButton(clearAction);
		toolBar.add(clearButton);

		Component toolBarSep = Box.createHorizontalGlue();
		toolBar.add(toolBarSep);

		serverLabel = new JLabel(host == null ? "" : host);
		toolBar.add(serverLabel);

		JPanel sendPanel = new JPanel();
		getContentPane().add(sendPanel, BorderLayout.SOUTH);
		sendPanel.setLayout(new BorderLayout(0, 0));
		sendTextField = new JTextField();
		sendTextField.setAction(sendAction);
		sendPanel.add(sendTextField);
		sendTextField.setColumns(10);

		JButton sendButton = new JButton(sendAction);
		sendPanel.add(sendButton, BorderLayout.EAST);

		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		JTextPane textPane = new JTextPane();
		textPane.setEditable(false);
		// autoscroll textPane to bottom
		DefaultCaret caret = (DefaultCaret) textPane.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		scrollPane.setViewportView(textPane);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		
		 //TODO menu
		JMenu connectionMenu = new JMenu("Connections");
		menuBar.add(connectionMenu);
		
		JMenu MessagesMenu = new JMenu("Messages");
		menuBar.add(MessagesMenu);
		
		JMenu UsersMenu = new JMenu("Users");
		menuBar.add(UsersMenu);
		
		JMenuItem quitMenuItem = new JMenuItem(quitAction);
		quitMenuItem.setName("Quit");
		connectionMenu.add(quitMenuItem);
		
		

		JMenuItem clearMenuItem = new JMenuItem(clearAction);
		MessagesMenu.add(clearMenuItem);

		JMenuItem clearSelectedItem = new JMenuItem("Clear Selected");
		clearSelectedItem.setAction(clearSelectionAction);
		UsersMenu.add(clearSelectedItem);
		
		JMenuItem rmuserItem = new JMenuItem(removeAction);
		UsersMenu.add(rmuserItem);
		
		
		JPanel leftPanel = new JPanel();
		leftPanel.setPreferredSize(new Dimension(200, 10));
		getContentPane().add(leftPanel, BorderLayout.WEST);
		leftPanel.setLayout(new BorderLayout(0, 0));


		

		JScrollPane listScrollPane = new JScrollPane();
		leftPanel.add(listScrollPane, BorderLayout.CENTER);
		
		JList<String> list = new JList<String>(elements);
		listScrollPane.setViewportView(list);
		list.setName("Elements");
		list.setBorder(UIManager.getBorder("EditorPane.border"));
		list.setSelectedIndex(0);
		list.setCellRenderer(new ColorTextRenderer());

		JPopupMenu popupMenu = new JPopupMenu();
		addPopup(list, popupMenu);

		/*JMenuItem mntmAdd = new JMenuItem(addAction);
		mntmAdd.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.META_MASK));
		popupMenu.add(mntmAdd);*/

		JMenuItem mntmRemove = new JMenuItem(removeAction);
		popupMenu.add(mntmRemove);

	

		JMenuItem mntmClearSelection = new JMenuItem(clearSelectionAction);
		popupMenu.add(mntmClearSelection);

		selectionModel = list.getSelectionModel();
		selectionModel.addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();

				int firstIndex = e.getFirstIndex();
				int lastIndex = e.getLastIndex();
				boolean isAdjusting = e.getValueIsAdjusting();
				/*
				 * isAdjusting remains true while events like drag n drop are
				 * still processed and becomes false afterwards.
				 */
				if (!isAdjusting)
				{
					output.append("Event for indexes " + firstIndex + " - "
						+ lastIndex + "; selected indexes:");

					if (lsm.isSelectionEmpty())
					{
						removeAction.setEnabled(false);
						clearSelectionAction.setEnabled(false);
						output.append(" <none>");
					}
					else
					{
						removeAction.setEnabled(true);
						clearSelectionAction.setEnabled(true);
						// Find out which indexes are selected.
						int minIndex = lsm.getMinSelectionIndex();
						int maxIndex = lsm.getMaxSelectionIndex();
						for (int i = minIndex; i <= maxIndex; i++)
						{
							if (lsm.isSelectedIndex(i))
							{
								output.append(" " + i);
							}
						}
					}
					output.append(newline);
				}
				else
				{
					// Still adjusting ...
					output.append("Processing ..." + newline);
				}
			}
		});

		// --------------------------------------------------------------------
		// Documents
		// récupération du document du textPane ainsi que du documentStyle et du
		// defaultColor du document
		//---------------------------------------------------------------------
		document = textPane.getStyledDocument();
		documentStyle = textPane.addStyle("New Style", null);
		defaultColor = StyleConstants.getForeground(documentStyle);


	}

	/**
	 * Affichage d'un message dans le {@link #document}, puis passage à la ligne
	 * (avec l'ajout de {@link Vocabulary#newLine})
	 * La partie "[yyyy/MM/dd HH:mm:ss]" correspond à la date/heure courante
	 * obtenue grâce à un Calendar et est affichée avec la defaultColor alors
	 * que la partie "utilisateur > message" doit être affichée avec une couleur
	 * déterminée d'après le nom d'utilisateur avec
	 * {@link #getColorFromName(String)}, le nom d'utilisateur est quant à lui
	 * déterminé d'après le message lui même avec {@link #parseName(String)}.
	 * @param message le message à afficher dans le {@link #document}
	 * @throws BadLocationException si l'écriture dans le document échoue
	 * @see {@link examples.widgets.ExampleFrame#appendToDocument(String, Color)}
	 * @see java.text.SimpleDateFormat#SimpleDateFormat(String)
	 * @see java.util.Calendar#getInstance()
	 * @see java.util.Calendar#getTime()
	 * @see javax.swing.text.StyleConstants
	 * @see javax.swing.text.StyledDocument#insertString(int, String,
	 * javax.swing.text.AttributeSet)
	 */
	public class AddAction extends AbstractAction
	{
		public AddAction()
		{
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.META_MASK));
			putValue(SMALL_ICON, new ImageIcon(ListExampleFrame.class.getResource("/examples/icons/add_user-16.png")));
			putValue(LARGE_ICON_KEY, new ImageIcon(ListExampleFrame.class.getResource("/examples/icons/add_user-32.png")));
			putValue(NAME, "Add...");
			putValue(SHORT_DESCRIPTION, "Add item");
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			output.append("Add action triggered" + newline);
			String inputValue = JOptionPane.showInputDialog("New item name");
			if (inputValue != null)
			{
				if (inputValue.length() > 0)
				{
					elements.addElement(inputValue);
				}
			}
		}
	}
	public class ClearSelectionAction extends AbstractAction
	{
		public ClearSelectionAction()
		{
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.META_MASK));
			putValue(LARGE_ICON_KEY, new ImageIcon(ListExampleFrame.class.getResource("/icons/delete_database-32.png")));
			putValue(SMALL_ICON, new ImageIcon(ListExampleFrame.class.getResource("/icons/delete_database-16.png")));
			//putValue(NAME, "Clear selection");
			putValue(SHORT_DESCRIPTION, "Unselect selected items");
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			output.append("Clear selection action triggered" + newline);
			selectionModel.clearSelection();
		}
	}
	
	public class RemoveItemAction extends AbstractAction
	{
		public RemoveItemAction()
		{
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.META_MASK));
			putValue(SMALL_ICON, new ImageIcon(ListExampleFrame.class.getResource("/examples/icons/remove_user-16.png")));
			putValue(LARGE_ICON_KEY, new ImageIcon(ListExampleFrame.class.getResource("/examples/icons/remove_user-32.png")));
			putValue(NAME, "Remove");
			putValue(SHORT_DESCRIPTION, "Removes item from list");
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			output.append("Remove action triggered for indexes : ");
			int minIndex = selectionModel.getMinSelectionIndex();
			int maxIndex = selectionModel.getMaxSelectionIndex();
			Stack<Integer> toRemove = new Stack<Integer>();
			for (int i = minIndex; i <= maxIndex; i++)
			{
				if (selectionModel.isSelectedIndex(i))
				{
					output.append(" " + i);
					toRemove.push(new Integer(i));
				}
			}
			output.append(newline);
			while (!toRemove.isEmpty())
			{
				int index = toRemove.pop().intValue();
				output.append("removing element: "
					+ elements.getElementAt(index) + newline);
				elements.remove(index);
			}
		}
	}
	private static void addPopup(Component component, final JPopupMenu popup)
	{
		component.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showMenu(e);
				}
			}

			private void showMenu(MouseEvent e)
			{
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}
	
	protected void writeMessage(String message) throws BadLocationException
	{
		/*
		 * ajout du message "[yyyy/MM/dd HH:mm:ss] utilisateur > message" à
		 * la fin du document avec la couleur déterminée d'après "utilisateur"
		 * (voir AbstractClientFrame#getColorFromName)
		 */
		StringBuffer sb = new StringBuffer();

		sb.append(message);
		sb.append(Vocabulary.newLine);

		// source et contenu du message avec la couleur du message
		String source = parseName(message);
		if ((source != null) && (source.length() > 0))
		{
			/*
			 * Changement de couleur du texte
			 */
			StyleConstants.setForeground(documentStyle,
			                             getColorFromName(source));
		}

		document.insertString(document.getLength(),
		                      sb.toString(),
		                      documentStyle);

		// Retour à la couleur de texte par défaut
		StyleConstants.setForeground(documentStyle, defaultColor);

	}

	/**
	 * Recherche du nom d'utilisateur dans un message de type
	 * "utilisateur > message".
	 * parseName est utilisé pour extraire le nom d'utilisateur d'un message
	 * afin d'utiliser le hashCode de ce nom pour créer une couleur dans
	 * laquelle
	 * sera affiché le message de cet utilisateur (ainsi tous les messages d'un
	 * même utilisateur auront la même couleur).
	 * @param message le message à parser
	 * @return le nom d'utilisateur s'il y en a un sinon null
	 */
	protected String parseName(String message)
	{
		/*
		 * renvoyer la chaine correspondant à la partie "utilisateur" dans
		 * un message contenant "utilisateur > message", ou bien null si cette
		 * partie n'existe pas.
		 */
		if (message.contains(">") && message.contains("]"))
		{
			int pos1 = message.indexOf(']');
			int pos2 = message.indexOf('>');
			try
			{
				return new String(message.substring(pos1 + 2, pos2 - 1));
			}
			catch (IndexOutOfBoundsException iobe)
			{
				logger.warning("ClientFrame::parseName: index out of bounds");
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	/**
	 * Recherche du contenu du message dans un message de type
	 * "utilisateur > message"
	 * @param message le message à parser
	 * @return le contenu du message s'il y en a un sinon null
	 */
	protected String parseContent(String message)
	{
		if (message.contains(">"))
		{
			int pos = message.indexOf('>');
			try
			{
				return new String(message.substring(pos + 1, message.length()));
			}
			catch (IndexOutOfBoundsException iobe)
			{
				logger
				    .warning("ClientFrame::parseContent: index out of bounds");
				return null;
			}
		}
		else
		{
			return message;
		}
	}

	/**
	 * Listener lorsque le bouton #btnClear est activé. Efface le contenu du
	 * {@link #document}
	 */
	protected class ClearAction extends AbstractAction
	{
		/**
		 * Constructeur d'une ClearAction : met en place le nom, la description,
		 * le raccourci clavier et les small|Large icons de l'action
		 */
		public ClearAction()
		{
			putValue(SMALL_ICON,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/erase2-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/erase2-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_L,
			                                InputEvent.META_MASK));
			//putValue(NAME, "Clear");
			putValue(SHORT_DESCRIPTION, "Clear document content");
		}

		/**
		 * Opérations réalisées lorsque l'action est sollicitée
		 * @param e évènement à l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{
			/*
			 * Effacer le contenu du document
			 */
			try
			{
				document.remove(0, document.getLength());
			}
			catch (BadLocationException ex)
			{
				logger.warning("ClientFrame: clear doc: bad location");
				logger.warning(ex.getLocalizedMessage());
			}
		}
	}

	/**
	 * Action réalisée pour envoyer un message au serveur
	 */
	protected class SendAction extends AbstractAction
	{
		/**
		 * Constructeur d'une SendAction : met en place le nom, la description,
		 * le raccourci clavier et les small|Large icons de l'action
		 */
		public SendAction()
		{
			putValue(SMALL_ICON,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/logout-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/logout-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_S,
			                                InputEvent.META_MASK));
			putValue(NAME, "Send");
			putValue(SHORT_DESCRIPTION, "Send text to server");
		}

		/**
		 * Opérations réalisées lorsque l'action est sollicitée
		 * @param e évènement à l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{
			/*
			 * récupération du contenu du textfield et envoi du message au
			 * serveur (ssi le message n'est pas vide), puis effacement du
			 * contenu du textfield.
			 */
			// Obtention du contenu du sendTextField
			String content = sendTextField.getText();

			// logger.fine("Le contenu du textField etait = " + content);

			// envoi du message
			if (content != null)
			{
				if (content.length() > 0)
				{
					sendMessage(content);

					// Effacement du contenu du textfield
					sendTextField.setText("");
				}
			}
		}
	}

	/**
	 * Action réalisée pour se délogguer du serveur
	 */
	private class QuitAction extends AbstractAction
	{
		/**
		 * Constructeur d'une QuitAction : met en place le nom, la description,
		 * le raccourci clavier et les small|Large icons de l'action
		 */
		public QuitAction()
		{
			putValue(SMALL_ICON,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/disconnected-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/disconnected-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_Q,
			                                InputEvent.META_MASK));
			//putValue(NAME, "Quit");
			putValue(SHORT_DESCRIPTION, "Disconnect from server and quit");
		}

		/**
		 * Opérations réalisées lorsque l'action "quitter" est sollicitée
		 * @param e évènement à l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{
			logger.info("QuitAction: sending bye ... ");

			serverLabel.setText("");
			thisRef.validate();

			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e1)
			{
				return;
			}

			sendMessage(Vocabulary.byeCmd);
		}
	}

	/**
	 * Classe gérant la fermeture correcte de la fenêtre. La fermeture correcte
	 * de la fenètre implique de lancer un cleanup
	 */
	protected class FrameWindowListener extends WindowAdapter
	{
		/**
		 * Méthode déclenchée à la fermeture de la fenêtre. Envoie la commande
		 * "bye" au serveur
		 */
		@Override
		public void windowClosing(WindowEvent e)
		{
			logger.info("FrameWindowListener::windowClosing: sending bye ... ");
			/*
			 * appeler actionPerformed de quitAction si celle ci est
			 * non nulle
			 */
			if (quitAction != null)
			{
				quitAction.actionPerformed(null);
			}
		}
	}

	/**
	 * Exécution de la boucle d'exécution. La boucle d'exécution consiste à lire
	 * une ligne sur le flux d'entrée avec un BufferedReader tant qu'une erreur
	 * d'IO n'intervient pas indiquant que le flux a été coupé. Auquel cas on
	 * quitte la boucle principale et on ferme les flux d'I/O avec #cleanup()
	 */
	@Override
	public void run()
	{
		try {
			inBR = new ObjectInputStream(inPipe);
		} catch (IOException e1) {
			logger.warning("no in");
		}

		Message messageIn;

		while (commonRun.booleanValue())
		{
			messageIn = null;
			/*
			 * - Lecture d'une ligne de texte en provenance du serveur avec inBR
			 * Si une exception survient lors de cette lecture on quitte la
			 * boucle.
			 * - Si cette ligne de texte n'est pas nulle on affiche le message
			 * dans le document avec le format voulu en utilisant
			 * #writeMessage(String)
			 * - Après la fin de la boucle on change commonRun à false de
			 * manière synchronisée afin que les autres threads utilisant ce
			 * commonRun puissent s'arrêter eux aussi :
			 * synchronized(commonRun)
			 * {
			 * commonRun = Boolean.FALSE;
			 * }
			 * Dans toutes les étapes si un problème survient (erreur,
			 * exception, ...) on quitte la boucle en ayant au préalable ajouté
			 * un "warning" ou un "severe" au logger (en fonction de l'erreur
			 * rencontrée) et mis le commonRun à false (de manière synchronisé).
			 */
			
			try
			{
				/*
				 * read from input (doit être bloquant)
				 */
				messageIn =(Message) inBR.readObject();
			}
			catch (IOException | ClassNotFoundException e)
			{
				logger.warning("ClientFrame: I/O Error reading");
				break;
			}

			if (messageIn != null)
			{
				// Ajouter le message à la fin du document avec la couleur
				// voulue
				try
				{
					String ouioui = new String(messageIn.toString());
					writeMessage(ouioui);
					String[] le_2= ouioui.split(" ");
					if(!elements.contains(le_2[2]))
					{
						elements.addElement(le_2[2]);
					}
				}
				catch (BadLocationException e)
				{
					logger.warning("ClientFrame: write at bad location: "
					    + e.getLocalizedMessage());
				}
				
			}
			else // messageIn == null
			{
				break;
			}
		}

		if (commonRun.booleanValue())
		{
			logger
			    .info("ClientFrame::cleanup: changing run state at the end ... ");
			synchronized (commonRun)
			{
				commonRun = Boolean.FALSE;
			}
		}

		cleanup();
	}

	/**
	 * Fermeture de la fenètre et des flux à la fin de l'exécution
	 */
	@Override
	public void cleanup()
	{
		logger.info("ClientFrame::cleanup: closing input buffered reader ... ");
		try
		{
			inBR.close();
		}
		catch (IOException e)
		{
			logger.warning("ClientFrame::cleanup: failed to close input reader"
			    + e.getLocalizedMessage());
		}

		super.cleanup();
	}
}

