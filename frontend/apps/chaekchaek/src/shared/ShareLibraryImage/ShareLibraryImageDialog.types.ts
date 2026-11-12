type Img = { title: string; spine: string | null; cover: string };

export type ShareLibraryImageDialogProps = {
  libraryName: string;
  imgs: Img[];
  onClose: () => void;
};
