type Img = { title: string; url: string };

export type ShareLibraryImageDialogProps = {
  libraryName: string;
  imgs: Img[];
  onClose: () => void;
};
