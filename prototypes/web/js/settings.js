'use strict';
/* global GymExports */
// GYM-022–024: Settings preview; real text downloads, with explicit native-storage boundaries.
const GymSettings = (() => {
  const state = { language: 'system', theme: 'system', saveFolderPreview: null };
  const words = {
    en: { more: 'More options', back: 'Back to previous screen', settings: 'Settings', train: 'Train', plan: 'Plan', progress: 'Progress', language: 'App language', system: 'System default', theme: 'Appearance', light: 'Light', dark: 'Dark', languageNote: 'Language preview: Settings and navigation. Other demo screens remain in English.', data: 'Your data', folder: 'Save location', noFolder: 'No folder selected', changeFolder: 'Change folder', folderNote: 'Folder selection is a preview. Files download through your browser.', status: 'Download status · JSON', statistics: 'Download statistics · CSV', details: 'About the files', filesNote: 'JSON keeps the complete demo state, including drafts, warm-ups and timer snapshots. CSV contains sample performance and attendance. Both are readable without the app.', guide: 'Download recovery guide · TXT', picker: 'Save folder preview', pickerNote: 'On Android, this opens the system folder picker.', documents: 'Documents / gym assystant', sdcard: 'SD card / gym assystant', cancel: 'Cancel', useFolder: 'Use this folder', ready: 'File handed to your browser. Check its downloads.', error: 'Download could not start. Please try again.' },
    it: { more: 'Altre opzioni', back: 'Torna alla schermata precedente', settings: 'Impostazioni', train: 'Allenamento', plan: 'Programma', progress: 'Progressi', language: 'Lingua dell’app', system: 'Predefinita del sistema', theme: 'Aspetto', light: 'Chiaro', dark: 'Scuro', languageNote: 'Anteprima lingua: impostazioni e navigazione. Le altre schermate demo restano in inglese.', data: 'I tuoi dati', folder: 'Cartella di salvataggio', noFolder: 'Nessuna cartella selezionata', changeFolder: 'Cambia cartella', folderNote: 'La cartella è un’anteprima. I file si scaricano tramite il browser.', status: 'Scarica stato · JSON', statistics: 'Scarica statistiche · CSV', details: 'Informazioni sui file', filesNote: 'Il JSON conserva lo stato completo della demo, incluse bozze, riscaldamento e tempi. Il CSV contiene prestazioni di esempio e presenze. Entrambi si leggono senza l’app.', guide: 'Scarica guida al recupero · TXT', picker: 'Anteprima cartella', pickerNote: 'Su Android si apre il selettore di cartelle del sistema.', documents: 'Documenti / gym assystant', sdcard: 'Scheda SD / gym assystant', cancel: 'Annulla', useFolder: 'Usa questa cartella', ready: 'File passato al browser. Controlla i download.', error: 'Impossibile avviare il download. Riprova.' },
    es: { more: 'Más opciones', back: 'Volver a la pantalla anterior', settings: 'Ajustes', train: 'Entrenar', plan: 'Plan', progress: 'Progreso', language: 'Idioma de la app', system: 'Predeterminado del sistema', theme: 'Apariencia', light: 'Claro', dark: 'Oscuro', languageNote: 'Vista previa del idioma: Ajustes y navegación. Las demás pantallas de la demo siguen en inglés.', data: 'Tus datos', folder: 'Carpeta de guardado', noFolder: 'Ninguna carpeta seleccionada', changeFolder: 'Cambiar carpeta', folderNote: 'La selección de carpeta es una vista previa. Los archivos se descargan mediante el navegador.', status: 'Descargar estado · JSON', statistics: 'Descargar estadísticas · CSV', details: 'Acerca de los archivos', filesNote: 'El JSON conserva todo el estado de la demo, incluidos borradores, series de calentamiento y tiempos. El CSV contiene rendimiento de ejemplo y asistencia. Ambos se leen sin la app.', guide: 'Descargar guía de recuperación · TXT', picker: 'Vista previa de carpeta', pickerNote: 'En Android se abre el selector de carpetas del sistema.', documents: 'Documentos / gym assystant', sdcard: 'Tarjeta SD / gym assystant', cancel: 'Cancelar', useFolder: 'Usar esta carpeta', ready: 'Archivo enviado al navegador. Comprueba las descargas.', error: 'No se pudo iniciar la descarga. Inténtalo de nuevo.' }
  };
  // GYM-022: match supported base languages across regional system variants.
  const locale = () => {
    const requested = state.language === 'system' ? navigator.language.toLowerCase().split('-')[0] : state.language;
    return Object.hasOwn(words, requested) ? requested : 'en';
  };
  const copy = key => words[locale()][key];
  function translate() {
    // The rest of the prototype is English; each translated region has its own lang.
    document.querySelectorAll('[data-settings-text]').forEach(element => { element.textContent = copy(element.dataset.settingsText); });
    for (const id of ['settings', 'folderDialog', 'overflowButton', 'overflowMenu', 'programNav', 'planNav', 'progressNav']) document.getElementById(id).lang = locale();
    document.getElementById('overflowButton').setAttribute('aria-label', copy('more'));
    document.getElementById('settingsBack').setAttribute('aria-label', copy('back'));
    document.getElementById('saveFolder').textContent = state.saveFolderPreview ?? copy('noFolder');
    document.getElementById('settingsFeedback').textContent = '';
  }
  function mount({ show, input }) {
    const byId = id => document.getElementById(id);
    const darkMode = matchMedia('(prefers-color-scheme: dark)');
    const applyTheme = () => {
      document.documentElement.dataset.theme = state.theme === 'system' ? darkMode.matches ? 'dark' : 'light' : state.theme;
    };
    darkMode.addEventListener('change', () => { if (state.theme === 'system') applyTheme(); });
    // GYM-022: one-item overflow menu, outside/Escape dismissal and exact-screen return.
    let returnView = 'program';
    let returnScroll = 0;
    const trigger = byId('overflowButton');
    const menu = byId('overflowMenu');
    const menuItem = byId('settingsMenuItem');
    function closeMenu(restoreFocus = false) {
      menu.hidden = true; trigger.setAttribute('aria-expanded', 'false');
      if (restoreFocus) trigger.focus();
    }
    function openMenu() {
      menu.hidden = false; trigger.setAttribute('aria-expanded', 'true'); menuItem.focus();
    }
    trigger.addEventListener('click', () => menu.hidden ? openMenu() : closeMenu(true));
    trigger.addEventListener('keydown', event => {
      if (['ArrowDown', 'ArrowUp'].includes(event.key)) { event.preventDefault(); openMenu(); }
    });
    menu.addEventListener('keydown', event => {
      if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) { event.preventDefault(); menuItem.focus(); }
    });
    document.addEventListener('keydown', event => {
      if (menu.hidden) return;
      if (event.key === 'Escape') { event.preventDefault(); closeMenu(true); }
      if (event.key === 'Tab') {
        event.preventDefault(); closeMenu();
        if (event.shiftKey) trigger.focus();
        else {
          const active = document.querySelector('[data-view]:not([hidden])');
          const next = [...active.querySelectorAll('button,input,select,summary')].find(el => !el.disabled && el.getClientRects().length);
          (next ?? byId('programNav')).focus();
        }
      }
    });
    document.addEventListener('pointerdown', event => {
      if (!menu.hidden && !menu.contains(event.target) && !trigger.contains(event.target)) closeMenu();
    });
    menuItem.addEventListener('click', () => {
      const active = document.querySelector('[data-view]:not([hidden])');
      if (active.id !== 'settings') { returnView = active.id; returnScroll = document.querySelector('.screen-area').scrollTop; }
      closeMenu(); show('settings', 'settingsBack');
    });
    byId('settingsBack').addEventListener('click', () => {
      show(returnView, 'overflowButton'); document.querySelector('.screen-area').scrollTop = returnScroll;
    });
    byId('appLanguage').addEventListener('change', event => { state.language = event.target.value; translate(); });
    byId('appTheme').addEventListener('change', event => { state.theme = event.target.value; applyTheme(); });
    byId('changeFolder').addEventListener('click', () => {
      byId('folderChoice').value = state.saveFolderPreview ?? 'Documents/gym assystant';
      byId('folderDialog').showModal();
    });
    byId('cancelFolder').addEventListener('click', () => byId('folderDialog').close());
    byId('useFolder').addEventListener('click', () => {
      state.saveFolderPreview = byId('folderChoice').value;
      byId('saveFolder').textContent = state.saveFolderPreview;
      byId('folderDialog').close();
    });
    byId('folderDialog').addEventListener('close', () => byId('changeFolder').focus());
    function download(kind) {
      try {
        const now = Date.now();
        const capture = input();
        const stamp = new Date(now).toISOString().replaceAll(':', '-');
        const content = kind === 'status' ? GymExports.json(capture, now) : kind === 'statistics' ? GymExports.statistics(capture) : GymExports.guide();
        const extension = kind === 'status' ? 'json' : kind === 'statistics' ? 'csv' : 'txt';
        const type = kind === 'status' ? 'application/json' : kind === 'statistics' ? 'text/csv' : 'text/plain';
        const url = URL.createObjectURL(new Blob([content], { type: `${type};charset=utf-8` }));
        const link = document.createElement('a');
        link.href = url; link.download = `gym-assystant-${kind}-${stamp}.${extension}`;
        document.body.append(link); link.click(); link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        byId('settingsFeedback').textContent = copy('ready');
      } catch (error) { byId('settingsFeedback').textContent = copy('error'); }
    }
    byId('downloadStatus').addEventListener('click', () => download('status'));
    byId('downloadStatistics').addEventListener('click', () => download('statistics'));
    byId('downloadGuide').addEventListener('click', () => download('guide'));
    translate(); applyTheme();
  }
  return { state, mount };
})();
