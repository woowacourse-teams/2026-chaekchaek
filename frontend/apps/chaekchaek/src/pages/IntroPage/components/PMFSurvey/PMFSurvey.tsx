import { useCallback, useState } from 'react';

import { FloatingButton, Icon, Shell, Surface } from '@chaekchaek/design-system';

import { getPmfSurvey } from '@/services/apis/pmfSurvey/repository';
import { useLoadData } from '@/services/core/useLoadData';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { PMFDialog } from '@/shared/surveys/PMF';

import styles from './PMFSurvey.module.css';

type IntroPageDialogType = 'PMFDialog';

export const PMFSurvey = () => {
  const { guest } = useAuthContext();

  const getPmfSurveyLoadData = useCallback(async () => {
    if (!guest) return await getPmfSurvey({});
    return await getPmfSurvey({}, { guestToken: guest.guestToken });
  }, [guest]);

  const {
    status: { data: pmfSurveyData },
  } = useLoadData({
    queryFn: getPmfSurveyLoadData,
  });

  const isVisible = pmfSurveyData && pmfSurveyData.eligible && !pmfSurveyData.alreadySubmitted;

  const [dialog, setDialog] = useState<IntroPageDialogType | null>(null);

  const handleOpenDialog = (dialog: IntroPageDialogType) => {
    setDialog(dialog);
  };

  const handleCloseDialog = () => {
    setDialog(null);
  };

  const renderDialog = (dialog: IntroPageDialogType | null) => {
    switch (dialog) {
      case 'PMFDialog':
        return (
          isVisible && (
            <PMFDialog
              questions={pmfSurveyData.questions}
              submissionToken={pmfSurveyData.submissionToken}
              onClose={handleCloseDialog}
            />
          )
        );
      default:
        return null;
    }
  };

  const dialogElement = renderDialog(dialog);

  return (
    isVisible && (
      <div className={styles[`scene-survey`]}>
        <Surface
          variant="primary"
          onClick={() => {
            handleOpenDialog('PMFDialog');
          }}
        >
          <Shell reverse>
            <Shell.Content
              leading="책췍 사용자 설문"
              title="췍췍, 어떠셨나요?"
              description="4개의 설문에 답해주세요"
            />
            <Shell.Trailing>
              <FloatingButton variant="secondary">
                <Icon.LongArrowUpRightIcon />
              </FloatingButton>
            </Shell.Trailing>
          </Shell>
        </Surface>

        {dialogElement}
      </div>
    )
  );
};
