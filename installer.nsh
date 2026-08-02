!macro customGUIInit
  # Aplicar Modo Oscuro a la ventana nativa de Windows
  # Color de Fondo: #121212 | Color de Texto: #FFFFFF
  SetCtlColors $HWNDPARENT 0xFFFFFF 0x121212

  # Estilizar el área del encabezado (MUI2)
  GetDlgItem $0 $HWNDPARENT 1037 # Fondo del Header
  SetCtlColors $0 0xFFFFFF 0x121212
  
  GetDlgItem $0 $HWNDPARENT 1038 # Título (Usamos el azul primario #0078D7)
  SetCtlColors $0 0x0078D7 0x121212

  GetDlgItem $0 $HWNDPARENT 1039 # Subtítulo
  SetCtlColors $0 0xAAAAAA 0x121212

  # Ocultar la línea divisoria clásica para un diseño más "Flat/Moderno"
  GetDlgItem $0 $HWNDPARENT 1034
  ShowWindow $0 0
!macroend

!macro customUnInstall
  # Mensaje de despedida personalizado
  MessageBox MB_ICONINFORMATION|MB_OK "Gracias por usar GLauncher. El creador DaniCraftYT25 espera verte de nuevo pronto."

  # Preguntar si se desea borrar la carpeta de datos (.glauncher)
  # Esto es útil para que el usuario elija si quiere conservar sus mundos y versiones
  MessageBox MB_YESNO "¿Deseas eliminar también todos tus datos de juego (Mundos, Versiones y Configuración) en .glauncher?$\n$\nAtención: Esta acción no se puede deshacer." IDNO +2
    RMDir /r "$APPDATA\.glauncher"

  DetailPrint "Limpiando rastros de GLauncher..."
!macroend