import butterupImport from "butteruptoasts";
import "butteruptoasts/src/butterup.css";

const butterup =
  butterupImport?.default ?? butterupImport?.butterup ?? butterupImport;

// Butterup 2 uses global options, not per-toast duration/maxToasts.
butterup.options.toastLife = 3000;
butterup.options.maxToasts = 3;

export const successToast = (message) => {
  butterup.toast({
    title: "Başarılı",
    message,
    type: "success",
    location: "top-right",
    icon: true,
    dismissable: true,
  });
};

export const errorToast = (message) => {
  butterup.toast({
    title: "Hata",
    message,
    type: "error",
    location: "top-right",
    icon: true,
    dismissable: true,
  });
};

export const warningToast = (message) => {
  butterup.toast({
    title: "Uyarı",
    message,
    type: "warning",
    location: "top-right",
    icon: true,
    dismissable: true,
  });
};

export const infoToast = (message) => {
  butterup.toast({
    title: "Bilgi",
    message,
    type: "info",
    location: "top-right",
    icon: true,
    dismissable: true,
  });
};
